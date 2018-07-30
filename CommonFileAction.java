package com.rkhd.ienterprise.apps.ingage.crm.file.action;

import com.rkhd.ienterprise.apps.ingage.crm.base.action.BaseAction;
import com.rkhd.ienterprise.apps.ingage.crm.util.FileUtil;
import com.rkhd.ienterprise.apps.twitter.file.model.TwitterFile;
import com.rkhd.ienterprise.apps.twitter.file.service.TwitterFileService;
import com.rkhd.ienterprise.base.commonfile.model.CommonFile;
import com.rkhd.ienterprise.base.commonfile.service.CommonFileService;
import com.rkhd.ienterprise.base.user.model.User;
import com.rkhd.ienterprise.base.user.service.UserService;
import com.rkhd.ienterprise.exception.ServiceException;
import com.rkhd.ienterprise.exception.WebException;
import com.rkhd.ienterprise.util.StringUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SONG
 * 2014-4-4.
 */
@Namespace("/json/sns_common_file")
public class CommonFileAction extends BaseAction {
    private static final String PAGES_ROOT = "/WEB-INF/pages/files/common";
    private static final String COMMON_PAGES_ROOT = "/WEB-INF/pages/common";

    private Integer systemId;
    private Long itemId;

    private Long fileId;
    private String jsonFiles;
    private String checkCode;
    private String downFileName;
    private InputStream inputStream;

    @Autowired
    private CommonFileService commonFileService;

    @Autowired
    private TwitterFileService twitterFileService ;
    @Autowired
    private UserService userService;

    @Action(value = "list",
            results = {@Result(name = SUCCESS, location = PAGES_ROOT + "/list.jsp")})
    public String list() {
        if (systemId == null || itemId == null) {
            setStatus(ERROR_CODE_SYSTEM);
            return SUCCESS;
        }
        try {
            List<CommonFile> files = commonFileService.getBySystemAndSystemItem(systemId, itemId, getTenantParam());

            List<Long> userIds = new ArrayList<>();
            Iterator<CommonFile> i = files.iterator();
            while(i.hasNext()){
                CommonFile file = i.next();
                userIds.add(file.getCreatedBy());
            }
            List<User> users = userService.getActiveUserListByIdList(userIds, getTenantParam());
            getRequest().setAttribute("userList", users);
            getRequest().setAttribute("fileList", files);
        } catch (ServiceException e) {
            setStatus(ERROR_CODE_SYSTEM);
            baseLog.error(e.getMessage());
        }
        return SUCCESS;
    }

    @Action(value = "save",
            results = {@Result(name = SUCCESS, location = COMMON_PAGES_ROOT + "/onlyData.jsp")})
    public String save() {
        if (StringUtil.isEmpty(jsonFiles) || systemId == null || itemId == null) {
            setStatus(ERROR_CODE_SYSTEM);
            return SUCCESS;
        }
        try {
            JSONArray array = JSONArray.fromObject(jsonFiles);
            for (Object o : array) {
                JSONObject file = JSONObject.fromObject(o);
                String fileName = file.optString("fileName");
                boolean isImg = FileUtil.isSupportedImage(file.optString("fileName"));

                CommonFile commonFile = new CommonFile();
                commonFile.setFilelength(file.optLong("fileLength"));
                commonFile.setFileUrl(file.optString("fileUrl"));
                commonFile.setFileName(fileName);
                commonFile.setFileType(isImg ? CommonFile.IMAGE_TYPE : CommonFile.FILE_TYPE);
                commonFile.setCreatedBy(getUserId());
                commonFile.setCreatedAt(System.currentTimeMillis());
                commonFile.setSystemItemId(itemId);
                commonFile.setSystemId(systemId);
                if (isImg) {
                    commonFile.setImgUrl(commonFile.getFileUrl());
                }
                commonFileService.saveAndConvertFile(commonFile, getTenantParam());
            }
        } catch (ServiceException e) {
            setStatus(ERROR_CODE_SYSTEM);
            baseLog.error(e.getMessage());
        } catch (Exception e) {
            setStatus(ERROR_CODE_SYSTEM);
            baseLog.error(e.getMessage(), e);
        }
        return SUCCESS;
    }


    @Action(value = "delete",
            results = {@Result(name = "success", location = COMMON_PAGES_ROOT + "/noData.jsp")})
    public String fileDelete() {
        try {
            CommonFile file = commonFileService.get(fileId, getTenantParam());
            validTenantModel(file);
            if (file == null) {
                setStatus(ERROR_CODE_NULL_OBJECT);
                return SUCCESS;
            }
            commonFileService.delete(fileId, getTenantParam());
        } catch (ServiceException e) {
            setStatus(ERROR_CODE_SYSTEM);
            baseLog.error(e.getMessage(), e);
        }
        return SUCCESS;
    }

    @Action(value = "download",
            results = {@Result(type = "stream", params = {"contentType", "application/octet-stream;charset=utf8", "inputName",
                    "inputStream", "contentDisposition", "${disposition};filename=\"${downFileName}\"", "bufferSize", "4096"})}
    )
    public String commonFileDownLoad() throws WebException {
        if (fileId == null || StringUtil.isEmpty(checkCode)) {
            setStatus(ERROR_CODE_SYSTEM);
            return SUCCESS;
        }
        try {
            if (!FileUtil.checkTfcode(fileId, checkCode)) {
                setStatus(ERROR_CODE_SYSTEM);
                return SUCCESS;
            }
            CommonFile file = commonFileService.get(fileId, getTenantParam());
            if (file == null) {
                // 产品文档处理：手机端存储文档的位置
                TwitterFile twitterFile = twitterFileService.get(fileId, getTenantParam());
                if ( twitterFile != null) {
                    downFileName = twitterFile.getFilename();
                    encodeFileName();
                    inputStream = (new URL(twitterFile.getFileurl()).openConnection().getInputStream());
                    return SUCCESS ;
                }
            }
            validTenantModel(file);
            if (file == null) {
                setStatus(ERROR_CODE_NULL_OBJECT);
                return SUCCESS;
            }
            downFileName = file.getFileName();
            encodeFileName();
            inputStream = (new URL(file.getFileUrl()).openConnection().getInputStream());
            commonFileService.incDownloadTimes(fileId, getTenantParam());
        } catch (ServiceException e) {
            setStatus(ERROR_CODE_SYSTEM);
            baseLog.error(e.getMessage());
        } catch (Exception e) {
            setStatus(ERROR_CODE_SYSTEM);
            baseLog.error(e.getMessage(), e);
        }
        return SUCCESS;
    }

    private void encodeFileName() throws UnsupportedEncodingException {
        baseLog.info("before conversion ------- " + downFileName);
        //对文件名中的特殊字符做相应的转码
        String userAgent = getRequest().getHeader("User-Agent");
        if (userAgent !=null) {
            userAgent = userAgent.toLowerCase();
            if (userAgent.indexOf("firefox") != -1) {
            //随便加的注释
            } else {
                downFileName = URLEncoder.encode(downFileName,"UTF-8");
            }
        }
        baseLog.info("after conversion ------- " + downFileName);
    }


    public Integer getSystemId() {
        return systemId;
    }

    public void setSystemId(Integer systemId) {
        this.systemId = systemId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getJsonFiles() {
        return jsonFiles;
    }

    public void setJsonFiles(String jsonFiles) {
        this.jsonFiles = jsonFiles;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public String getDownFileName() throws UnsupportedEncodingException {
        return new String(downFileName.getBytes("GBK"), "iso-8859-1");
    }

    public void setDownFileName(String downFileName) {
        this.downFileName = downFileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}

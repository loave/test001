#coding=utf-8
import random
# ------------------------随机电话----------------------------
# list1=[]
# file1=open('rmobile.txt','w+')
# i=0
# while i <4000:
#     if random.randrange (13000000000,13999999999 ,1) not in list1:
#         list1.append(random.randrange (13000000000,13999999999 ,1))
#         file1.write(str(list1[i]))
#         file1.write('\n')
#         i+=1
# file1.close()


# ------------------------随机邮箱----------------------------
# list2=[]
# file2=open('remail.txt','w+')
# list3=['@163.com','@qq.com','@126.com','@sina.com','@sohu.com']   #邮箱后缀
# i=0
# while i <4000:
#     if random.randrange (1568125,999999999,1) not in list2:
#         list2.append(random.randrange (1568125,999999999,1))  #邮箱前缀
#         file2.write(str(list2[i])+random.choice(list3))
#         file2.write('\n')
#         i+=1
# file2.close()

# # ------------------------随机生日----------------------------
# file3=open('rbirth.txt','w+')
#
# for i in range(0,4000):
#     file3.write(str(random.randrange(1968,1988,1))+'-'+str(random.randrange(1,12,1))+'-'+str(random.randrange(1,28,1)))
#     file3.write('\n')
# file3.close()

# # # ------------------------随机名字----------------------------
fn=[]
sn=[]
name=[]
fnfile=open('fname.txt','r')
snfile=open('sname.txt','r')
for line in fnfile.readlines():
    fn.append(line.split('\n')[0])
for line in snfile.readlines():
    sn.append(line.split('\n')[0])
rnf=open('rname.txt','w+')
rname=''
i=0
while i <4000:
    rname = random.choice(fn) + random.choice(sn)
    if rname not in name:
        rnf.write(rname)
        rnf.write('\n')
        name.append(rname)
        i+=1
rnf.close()
fnfile.close()
snfile.close()
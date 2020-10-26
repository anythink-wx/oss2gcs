# oss2gcs
将阿里oss的数据同步到谷歌gcs。

从OSS同步到谷歌的GCS一直没有一个比较方便的方案，使用rsync挂载两边的文件系统速度太慢，所以写一个脚本通过阿里云OSS的事件通知方式来解决。


# 使用方式

1 创建事件通知

阿里云对象存储点击存储桶，顶部菜单选择事件通知。点击创建规则。

事件类型实现了  PutObject，DeleteObject，DeleteObjects，PostObject。根据自己需要选择，

接受终端选择队列，输入名称，确定。

记录对象存储的 EndPoint地域节点，建议开通传输加速域名后使用 oss-accelerate.aliyuncs.com 。

2 配置消息服务 MNS

点击队列，创建队列，队列名称起一个名字比如abc，其他不用填确认保存。

点击右上角 获取 Endpoint 记住。


点击主题，点击订阅详情。

右上角点击创建订阅。推送类型选择队列，订阅名称随意起，接收端地址填刚才队列名称比如abc，推送格式默认SIMPLIFIED。



# 配置文件

~~~
默认为 config.propetries 和 jar 文件同级

account_id = 阿里云账号的ID
account_key = 阿里云账号的KEY

queue_end_point = 队列的 Endpoint 地址
queue_ref = 队列的名称 如123

oss_bucket = OSS存储桶名称
oss_end_point = OSS存储桶 Endpoint



gsutil_cmd = /bin/gsutil (谷歌的gsutil命令行绝对地址)
gcs_bucket=gcs_bucket
        
ThreadNum=10 启动的线程数量
~~~

# 启动方式
因为队列是https协议的，时间长了阿里云会中断连接，导致进程等卡死，所以修改了下代码，需要使用守护进程方式配置
~~~
java -jar change-file-1.0-SNAPSHOT.jar
~~~

# 守护进程


~~~
supervisor.ini

[program:rsync]
command=/home/cocosplay1/jdk-11.0.5/bin/java -jar change-file-1.0-SNAPSHOT.jar
directory=/home/cocosplay1/sync/
autorestart=true
stdout_logfile=/www/server/panel/plugin/supervisor/log/rsync.out.log
redirect_stderr=true
user=root （可选）
priority=999
~~~


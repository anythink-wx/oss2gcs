import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.common.ServiceException;
import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.QueueMeta;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.aliyun.mns.client.Utils.logger;

public class ChangeFile {

    protected static OSS ossClient = null;
    protected static Properties config = null;


    protected static boolean parseConf() {

        String configName = System.getProperty("configName", "config.properties");
        String confFilePath = System.getProperty("user.dir") + System.getProperty("file.separator") + configName;

        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(confFilePath));
            if (bis == null) {
                System.out.println("ConfFile not opened: " + confFilePath);
                return false;
            }
        } catch (FileNotFoundException e) {
            System.out.println("ConfFile not found: " + confFilePath);
            return false;
        }

        // load file
        Properties properties = new Properties();
        try {
            properties.load(bis);

            config = properties;
        } catch (IOException e) {
            System.out.println("Load ConfFile Failed: " + e.getMessage());
            return false;
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                // do nothing
            }
        }

        return true;
    }

    public static void main(String[] args) {

        if (!parseConf()) {
            System.exit(1);
        }

        String account_id = config.getProperty("account_id");
        String account_key = config.getProperty("account_key");
        String queue_end_point = config.getProperty("queue_end_point");
        String oss_end_point = config.getProperty("oss_end_point");


        // config.getProperty("");

        ossClient = new OSSClientBuilder().build(oss_end_point, account_id, account_key);
        CloudAccount cloudAccount = new CloudAccount(account_id, account_key, queue_end_point);
        MNSClient client = cloudAccount.getMNSClient(); // 在程序中，CloudAccount以及MNSClient单例实现即可，多线程安全


        int threadNum = Integer.parseInt(config.getProperty("ThreadNum"));
        ExecutorService threadPool = Executors.newFixedThreadPool(threadNum);
        System.out.println(Thread.currentThread().getName() + " has been start");


        try {
            //System.out.println("message handle: " + popMsg.getReceiptHandle());
            // 默认会做 base64 解码
            // /System.out.println("message body: " + popMsg.getMessageBodyAsString());
            // 消息 body 的原始数据，不做 base64 解码
            // System.out.println("message body: " + popMsg.getMessageBodyAsRawString ());
            //System.out.println("message dequeue count:" + popMsg.getDequeueCount());
            for (int i = 0; i < threadNum; i++) {
                new Thread(()->{
                    while (true) {

                        CloudQueue queue = client.getQueueRef(config.getProperty("queue_ref"));
                        Message popMsg = queue.popMessage();

                        try {
                            if (popMsg != null) {
                                new Parse(popMsg.getMessageBodyAsString());
                                queue.deleteMessage(popMsg.getReceiptHandle());
                                System.out.println(Thread.currentThread().getName() + " 删除消息:" + popMsg.getReceiptHandle());
                            } else {
                                Thread.sleep(2000);
                            }

                        } catch (OSSException oe) {

                            if (oe.getErrorCode() != null) {
                                if (oe.getErrorCode().equals("NoSuchKey")) {
                                    queue.deleteMessage(popMsg.getReceiptHandle());
                                } else {
                                    System.out.println("OSSException:" + oe.getErrorCode());
                                    oe.printStackTrace();
                                }
                            }


                        } catch (ServiceException se) {
                            if (se.getErrorCode() != null) {
                                if (se.getErrorCode().equals("MessageNotExist")) {
                                    queue.deleteMessage(popMsg.getReceiptHandle());
                                } else {
                                    System.out.println("ServiceException:" + se.getErrorCode());
                                    se.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Unknown exception happened!");
                            e.printStackTrace();
                        }
                    }
                }).start();
                System.out.println("create thread: " + i);
            }
        } catch (ClientException ce) {
            System.out.println("ClientException:" + ce.getCause().getMessage());
            ce.printStackTrace();
        } catch (ServiceException se) {
            logger.error("MNS exception requestId:" + se.getRequestId(), se);
            if (se.getErrorCode() != null) {
                if (se.getErrorCode().equals("QueueNotExist")) {
                    System.out.println("Queue is not exist.Please create before use");
                } else if (se.getErrorCode().equals("TimeExpired")) {
                    System.out.println("The request is time expired. Please check your local machine timeclock");
                }
            }
            se.printStackTrace();
        } catch (Exception e) {
            System.out.println("Unknown exception happened!");
            e.printStackTrace();
        }

    }


    @Test
    public void test() throws IOException {

        parseConf();
        String account_id = config.getProperty("account_id");
        String account_key = config.getProperty("account_key");
        String queue_end_point = config.getProperty("queue_end_point");
        String oss_end_point = config.getProperty("oss_end_point");

        ossClient = new OSSClientBuilder().build(oss_end_point, account_id, account_key);


        String test = "{\"events\": [{\n" +
                "            \"eventName\": \"ObjectCreated:PostObject\",\n" +
                "            \"eventSource\": \"acs:oss\",\n" +
                "            \"eventTime\": \"2019-11-28T16:08:59.000Z\",\n" +
                "            \"eventVersion\": \"1.0\",\n" +
                "            \"oss\": {\n" +
                "                \"bucket\": {\n" +
                "                    \"arn\": \"acs:oss:cn-qingdao:1206248126365860:chukong\",\n" +
                "                    \"name\": \"chukong\",\n" +
                "                    \"ownerIdentity\": \"1206248126365860\",\n" +
                "                    \"virtualBucket\": \"\"},\n" +
                "                \"object\": {\n" +
                "                    \"deltaSize\": 807102,\n" +
                "                    \"eTag\": \"2C7AA112DF362FABFC5179F5CD301E92\",\n" +
                "                    \"key\": \"uploads/201911/jpg/2c7aa112df362fabfc5179f5cd301e92.jpg\",\n" +
                "                    \"size\": 807102},\n" +
                "                \"ossSchemaVersion\": \"1.0\",\n" +
                "                \"ruleId\": \"change-file\"},\n" +
                "            \"region\": \"cn-qingdao\",\n" +
                "            \"requestParameters\": {\"sourceIPAddress\": \"120.239.196.184\"},\n" +
                "            \"responseElements\": {\"requestId\": \"5DDFF11A400155383182F60B\"},\n" +
                "            \"userIdentity\": {\"principalId\": \"1206248126365860\"}}]}";

        Parse parse = new Parse(test);
    }
}

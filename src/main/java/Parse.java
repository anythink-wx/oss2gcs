import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;

import java.io.*;
import java.util.Arrays;

class Parse {


    private static String gsutil_cmd = ChangeFile.config.getProperty("gsutil_cmd");
    private static String gcs_bucket = ChangeFile.config.getProperty("gcs_bucket");


    Parse(String Message) throws IOException {

        //System.out.println(Message);

        JSONObject jsonObject = JSON.parseObject(Message);
        String events = jsonObject.getString("events");
        JSONArray objects = JSON.parseArray(events);


        for (Object obj : objects) {

            JSONObject jsonObject1 = JSON.parseObject(obj.toString());
            String eventName = jsonObject1.getString("eventName");
            String oss = jsonObject1.getString("oss");
            JSONObject OssObj = JSON.parseObject(oss);
            String oss_item = OssObj.getString("object");

            JSONObject oss_itemObj = JSON.parseObject(oss_item);
            String key = oss_itemObj.getString("key");
            System.out.println("获取事件:" + eventName + ", key:" + key);

//            String exclude = ChangeFile.config.getProperty("exclude");
//            if (exclude != null){
//                String[] split = exclude.split(",");
//                int i = Arrays.binarySearch(split, key);
//                System.out.println("search index:" + i);
//                if(i >= 0){
//                    System.out.println("目录被排除:" + key + " " + i);
//                    continue;
//                }
//            }
            syncFile(eventName, key);
        }
        // System.out.println(objects);
    }


    private void syncFile(String action, String key) throws IOException {
        switch (action) {
            case "ObjectCreated:PostObject":
            case "ObjectCreated:PutObject":
                OSSObject bucket = ChangeFile.ossClient.getObject(ChangeFile.config.getProperty("oss_bucket"), key);
                InputStream objectContent = bucket.getObjectContent();
                downLoadFile(objectContent, key);
                objectContent.close();
                break;
            case "ObjectRemoved:DeleteObject":
            case "ObjectRemoved:DeleteObjects":
                String cmd = gsutil_cmd + " rm gs://" + gcs_bucket + "/" + key;
                rumCmd(cmd);
                break;
            case "ObjectCreated:CopyObject":
                cmd = gsutil_cmd + " cp gs://" + gcs_bucket + "/" + key;
                rumCmd(cmd);
                break;
        }

    }

    private void downLoadFile(InputStream objectContent, String key) throws IOException {

        String path = key.substring(0, key.lastIndexOf("/") + 1);
        //String filename = key.substring(key.lastIndexOf("/") + 1, key.length());

        File file = new File(path);
        if (!file.isDirectory()) {
            file.mkdirs();
        }

        File file_path = new File(key);

        boolean exists = file_path.exists();
        if (exists) {
            GoogleSync(key);
            System.out.println("本地存在:" + key + " 已直接返回 ");
            return;
        }

        BufferedOutputStream out = null;
        out = new BufferedOutputStream(new FileOutputStream(file_path));
        if (objectContent != null) {
            BufferedInputStream in = new BufferedInputStream(objectContent);
            int len = -1;
            byte[] b = new byte[1024];
            while ((len = in.read(b)) != -1) {
                out.write(b, 0, len);
            }
            in.close();
            out.close();
        }
        System.out.println("下载文件:" + key);
        GoogleSync(key);
    }

    private void GoogleSync(String key) {

        try {


//            // 设置http访问要使用的代理服务器的地址
//            prop.setProperty("http.proxyHost", "127.0.0.1");
//            // 设置http访问要使用的代理服务器的端口
//            prop.setProperty("http.proxyPort", "7890");
//            // 设置安全访问使用的代理服务器地址与端口
//            // 它没有https.nonProxyHosts属性，它按照http.nonProxyHosts 中设置的规则访问
//            prop.setProperty("https.proxyHost", "127.0.0.1");
//            prop.setProperty("https.proxyPort", "7890");
//            // 使用ftp代理服务器的主机、端口以及不需要使用ftp代理服务器的主机
//            // socks代理服务器的地址与端口
//            prop.setProperty("socksProxyHost", "127.0.0.1");
//            prop.setProperty("socksProxyPort", "7891");

            File file = new File("");
            String basePath = file.getCanonicalPath();
            System.out.println(basePath);

            String cmd = gsutil_cmd + " cp " + basePath + "/" + key + " gs://" + gcs_bucket + "/" + key;
            rumCmd(cmd);
            setMime(key);
            clean(key);

        } catch (IOException e) {
            System.out.println("命令执行失败:" + e.getCause().getMessage());
            e.printStackTrace();
        }
    }

    private void rumCmd(String cmd) {
        System.out.println("执行命令:" + cmd);

        Process p;
        BufferedReader br = null;

        try {
            p = Runtime.getRuntime().exec(cmd);
            String line;

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = reader.readLine()) != null) {
                System.out.println("getInputStream: " + line);
            }

            line = null;
            br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException e) {
            System.out.println("命令执行失败:" + e.getCause().getMessage());
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //System.out.println("执行结束");
    }

    private void setMime(String key) {
        String suffix = key.substring(key.lastIndexOf(".") + 1).toLowerCase();
        String[] suffix_defined = {"png", "jpg", "js", "html"};

        if (Arrays.binarySearch(suffix_defined, suffix) > -1) {

            String mime = null;
            switch (suffix) {
                case "jpg":
                    mime = "content-type:image/jpeg";
                    break;

                case "png":
                    mime = "content-type:image/png";
                    break;
                case "js":
                    mime = "content-type:application/json";
                    break;

                case "html":
                    mime = "content-type:text/html";
                    break;
            }

            String cmd = gsutil_cmd + " -m setmeta -h " + mime + " gs://" + gcs_bucket + "/" + key;
            rumCmd(cmd);
        }

    }

    void clean(String key) {
        File file = new File(key);
        boolean delete = file.delete();
    }
}

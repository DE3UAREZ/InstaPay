package eg.com.adcb.IPN.utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;

public class Request {
    public static String SendRequest(String endpoint, String soapAction, String body) throws Exception {
        CloseableHttpClient httpclient = HttpClients
                .custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        HttpPost httpPost = new HttpPost(endpoint);
        httpPost.setHeader("SOAPAction", soapAction);
        httpPost.setHeader("Content-Type", "*/*");

//        if (endpoint.contains("femi")) {
//            httpPost.setHeader("Content-Type", "*/*");
//        } else {
//            httpPost.setHeader("Content-Type", "application/soap+xml");
//        }
//        httpPost.setHeader("Accept", "application/json");
        httpPost.setEntity(new StringEntity(body));
        HttpResponse httpResponse = httpclient.execute(httpPost);
        return EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
    }
}

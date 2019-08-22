package au.org.massive.strudel_web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import javax.net.ssl.*;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
/**
 * Provides settings for the application. Requires "nimportal.properties" to be in the class path.
 * Settings objects are singletons, and configuration is loaded only once.
 *
 * @author jrigby
 */
public class Settings {

    private Map<String, Map<String, String>> oidConfig;
    private static Settings instance;
    private String configurationName; 
    private Configuration config;
    private List<String> providers = null;
    private MongoDatabase database = null;
    private Settings(){
        try {
            config = new PropertiesConfiguration("coesra.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        oidConfig = new HashMap<String, Map<String, String>>();
        // Turn of SSL cert verification if requested
        if (config.getBoolean("allow-invalid-ssl-cert", false)) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null,
                        new TrustManager[] {
                                new X509TrustManager() {
                                    @Override
                                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                                    }

                                    @Override
                                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                                    }

                                    @Override
                                    public X509Certificate[] getAcceptedIssuers() {
                                        return null;
                                    }
                                }
                        },
                        new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            MongoClient client = new MongoClient("localhost", 27017);
    		database = client.getDatabase("settings-db");
        }

        // get the oidc configs
        providers = new ArrayList<String>();
        int configCount = config.getInt("oidc-count");
        for(int index=0; index< configCount; index++) {
        	Map<String, String> aConfig= new HashMap<String, String>();
        	String provider = config.getString("oidc-name-"+index);
        	providers.add(provider);
        	aConfig.put("oidc-name", provider);
        	aConfig.put("oidc-endpoint", config.getString("oidc-endpoint-"+index));
        	aConfig.put("oidc-token-endpoint", config.getString("oidc-token-endpoint-"+index));
        	aConfig.put("oidc-client-id", config.getString("oidc-client-id-"+index));
        	aConfig.put("oidc-client-secret", config.getString("oidc-client-secret-"+index));
        	aConfig.put("oidc-redirect", config.getString("oidc-redirect-"+index));
        	aConfig.put("oidc-issuer", config.getString("oidc-issuer-"+index));
        	aConfig.put("oidc-jwkUrl", config.getString("oidc-jwkUrl-"+index));
        	oidConfig.put(provider, aConfig);
        	if(index == 0) {
        		oidConfig.put("default", aConfig);
        	}
        }
        configurationName = config.getString("configuration-name", "CoESRA");
    }

    public static Settings getInstance(){
        if (instance == null) {
            try {
				instance = new Settings();
			} catch (Exception e) {
				return null;
			}
        }
        return instance;
    }

    public String getConfigurationName() {
    	return this.configurationName;
    }
   
    public boolean isDefaultOidcProvider(String providerName){
    	return providerName.equals(this.getDefaultProvider());
    }
    
    public String getDefaultProvider() {
    	return providers.get(0);
    }
    
    public List<String> getNonDefaultProvider(){
    	return providers.subList(1, providers.size());
    }
    
    public Map<String, String> getOidConfig(String name){
    	return this.oidConfig.get(name);
    }
    
    public String getOidConfigValue(String name, String field) {
    	Map<String, String> config =  this.getOidConfig(name);
    	if(config !=null) {
    		return config.get(field);
    	}
    	return null;
    }
    
    public MongoDatabase getStorage() {
    	return database;
    }
}

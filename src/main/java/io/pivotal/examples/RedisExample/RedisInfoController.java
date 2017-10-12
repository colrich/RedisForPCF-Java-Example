
package io.pivotal.examples.RedisExample;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;

@RestController
public class RedisInfoController {

    private Logger LOG = Logger.getLogger(RedisInfoController.class.getName());
    private Jedis jedis = null;

    @RequestMapping("/")
    public RedisInstanceInfo getInfo() {
        LOG.log(Level.WARNING, "Getting Redis Instance Info in Spring controller...");
        // first we need to get the value of VCAP_SERVICES, the environment variable
        // where connection info is stored
        String vcap = System.getenv("VCAP_SERVICES");
        LOG.log(Level.WARNING, "VCAP_SERVICES content: " + vcap);


        // now we parse the json in VCAP_SERVICES
        LOG.log(Level.WARNING, "Using GSON to parse the json...");
        JsonElement root = new JsonParser().parse(vcap);
        JsonObject redis = root.getAsJsonObject().get("p.redis").getAsJsonArray().get(0).getAsJsonObject();
        LOG.log(Level.WARNING, "instance name: " + redis.get("name").getAsString());

        // then we pull out the credentials block and produce the output
        JsonObject creds = redis.get("credentials").getAsJsonObject();
        RedisInstanceInfo info = new RedisInstanceInfo();
        info.setHost(creds.get("host").getAsString());
        info.setPort(creds.get("port").getAsInt());
        info.setPassword(creds.get("password").getAsString());

        // the object will be json serialized automatically by Spring web - we just need to return it
        return info;
    }

    @RequestMapping("/set")
    public String setKey(@RequestParam("kn") String key, @RequestParam("kv") String val) {
        LOG.log(Level.WARNING, "Called the key set method, going to set key: " + key + " to val: " + val);

        if (jedis == null || !jedis.isConnected()) {
            jedis = getJedisConnection();
        }
        jedis.set(key, val);

        return "Set key: " + key + " to value: " + val;
    }

    @RequestMapping("/get")
    String getKey(@RequestParam("kn") String key) {
        LOG.log(Level.WARNING, "Called the key get method, going to return val for key: " + key);

        if (jedis == null || !jedis.isConnected()) {
            jedis = getJedisConnection();
        }

        return jedis.get(key);
    }

    private Jedis getJedisConnection() {
        // get our connection info from VCAP_SERVICES
        RedisInstanceInfo info = getInfo();
        Jedis jedis = new Jedis(info.getHost(), info.getPort());
        
        // make the connection
        jedis.connect();

        // authorize with our password
        jedis.auth(info.getPassword());

        return jedis;
    }

}


//{ \"p.redis\": [{ \"credentials\": { \"host\": \"10.77.0.13\", \"password\": \"MvArPhml0MYYts7iw+ccBLAiEn0=\", \"port\": 6379}, \"label\": \"p.redis\", \"name\": \"croredis\", \"plan\": \"cache-small\", \"provider\": null, \"syslog_drain_url\": null, \"tags\": [  \"redis\", \"pivotal\", \"on-demand\", \"session-replication\" ], \"volume_mounts\": [] }]}
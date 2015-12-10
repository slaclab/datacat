package org.srs.datacat.client;

import com.google.common.base.CaseFormat;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;

/**
 * Configuration helper class.
 * @author bvan
 */
public final class Config {

    private Config(){}
    
    /**
     * Load the [defaults] section from from a default config file at ~/.datacat/default.cfg.
     *
     * @return A config map
     */
    public static Map<String, String> defaultConfig() throws IOException, ConfigurationException{
        return defaultConfig(null);
    }

    /**
     * Load the [defaults] section and override properties found there with properties found in the
     * overrideSection.
     *
     * @param overrideSection Section with which to override the properties found under [defaults]
     * @return A config map
     */
    public static Map<String, String> defaultConfig(String overrideSection) throws IOException, 
            ConfigurationException{
        Path path = expandUser("~/.datacat/default.cfg");
        return configFromFile(path, overrideSection);
    }

    /**
     * Load configuration from file. Use only the [defaults] section.
     *
     * @param filePath File to use instead of the default file.
     * @return A config map.
     */
    public static Map<String, String> configFromFile(Path filePath) throws IOException, 
            ConfigurationException{
        return configFromFile(expandUser(filePath.toString()), null);
    }

    /**
     * Load configuration from file. Use only the [defaults] section.
     *
     * @param filePath File to use instead of the default file.
     * @param overrideSection Section with which to override the properties found under [defaults]
     * @return A config map.
     */
    public static Map<String, String> configFromFile(Path filePath, String overrideSection) throws IOException, 
            ConfigurationException{
        HierarchicalINIConfiguration ini;
        try (Reader reader = Files.newBufferedReader(filePath)) {
            ini = new HierarchicalINIConfiguration();
            ini.load(reader);
        }
        HashMap<String, String> configMap = new HashMap<>();

        SubnodeConfiguration defaults = ini.getSection("defaults");
        for(Iterator<String> iter = defaults.getKeys(); iter.hasNext();){
            String key = iter.next();
            String value = defaults.getString(key);
            configMap.put(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key), value);
        }

        if(overrideSection != null){
            SubnodeConfiguration override = ini.getSection(overrideSection);
            for(Iterator<String> iter = override.getKeys(); iter.hasNext();){
                String key = iter.next();
                String value = override.getString(key);
                configMap.put(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key), value);
            }
        }
        return configMap;
    }

    /**
     * Based on Python's expanduser, probably doesn't work on Windows.
     *
     * @param path path string to (potentially) expand with home directory.
     * @return
     */
    public static Path expandUser(String path){
        if(path.charAt(0) != '~'){
            return Paths.get(path);
        }
        int i = 1;
        // Consume to end if necessary
        while(i < path.length() && '/' != path.charAt(i)){
            i++;
        }
        Path homePath = Paths.get(System.getProperty("user.home"));

        if(i != 1){
            String user = path.substring(1, i);
            homePath = homePath.getParent().resolve(user);
        }
        return homePath.resolve(path.substring(i + 1));
    }

}

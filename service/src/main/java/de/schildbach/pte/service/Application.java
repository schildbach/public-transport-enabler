/*
 * Copyright 2012-2019 the original author or authors.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.service;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.schildbach.pte.AbstractNetworkProvider;

/**
 * @author Felix Delattre
 */
public class Application {

    public static AbstractNetworkProvider get_provider() {

        /* Read configuration about provider and additional information related from config.json */
        String provider = "", token = "";
        JSONParser parser = new JSONParser();
        try (Reader reader = new FileReader("service/config.json")) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            provider = (String) jsonObject.get("provider");
            token = (String) jsonObject.get("token");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        /* Create Provider object from dynamic class name obtained from configuration above */
        Object object = null;
        try {
            Class<?> classDefinition = Class.forName("de.schildbach.pte." + provider + "Provider");

            if (token != "") {
                Constructor<?> classConstructor = classDefinition.getConstructor(String.class);
                object = classConstructor.newInstance(new Object[] { token });
            }
            else {
                object = classDefinition.newInstance();
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    
        return (AbstractNetworkProvider) object;
    }
}


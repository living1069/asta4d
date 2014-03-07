/*
 * Copyright 2012 astamuse company,Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.astamuse.asta4d.web.util.redirect;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.Base64;

import com.astamuse.asta4d.util.IdGenerator;
import com.astamuse.asta4d.web.WebApplicationConfiguration;

public class RedirectUtil {

    private final static SecureRandom sr;
    static {
        // use the last 32 bit of current time as the seed
        ByteBuffer bb = ByteBuffer.allocate(64);
        bb.putLong(System.nanoTime());
        byte[] bytes = bb.array();
        byte[] seed = new byte[4];
        System.arraycopy(bytes, 4, seed, 0, 4);
        sr = new SecureRandom(seed);
    }

    private static final String KEY_FLASH_SCOPE_ID = WebApplicationConfiguration.getWebApplicationConfiguration()
            .getFlashScopeForwardParameterName();

    private static String createEncryptedFlashScopeId() {
        try {
            String flashScopeId = IdGenerator.createId();

            byte[] idBytes = flashScopeId.getBytes();
            ByteBuffer bb = ByteBuffer.allocate(idBytes.length + 4);
            bb.put(idBytes);

            // add random salt
            bb.putInt(sr.nextInt());

            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            return Base64.encodeBase64URLSafeString(crypt.digest(bb.array()));

        } catch (NoSuchAlgorithmException e) {
            // impossible
            throw new RuntimeException(e);
        }
    }

    public static String setFlashScopeData(String url, Map<String, Object> flashScopeData) {
        if (flashScopeData == null || flashScopeData.isEmpty()) {
            return url;
        }
        String flashScopeId = createEncryptedFlashScopeId();
        FlashScopeDataManager.getInstance().put(flashScopeId, flashScopeData);
        if (url.contains("?")) {
            return url + '&' + KEY_FLASH_SCOPE_ID + '=' + flashScopeId;
        } else {
            return url + '?' + KEY_FLASH_SCOPE_ID + '=' + flashScopeId;
        }
    }

    public static Map<String, Object> retrieveFlashScopeData(HttpServletRequest request) {

        String flashScopeId = request.getParameter(RedirectUtil.KEY_FLASH_SCOPE_ID);
        if (StringUtils.isEmpty(flashScopeId)) {
            return Collections.emptyMap();
        } else {
            return FlashScopeDataManager.getInstance().get(flashScopeId);
        }

    }

}

/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2017 The PWM Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package password.pwm.svc.token;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import password.pwm.bean.UserIdentity;
import password.pwm.util.java.JavaHelper;
import password.pwm.util.java.JsonUtil;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
public class TokenPayload implements Serializable {
    private final Instant date;
    private final String name;
    private final Map<String,String> data;

    @SerializedName("user")
    private final UserIdentity userIdentity;
    private final Set<String> dest;
    private final String guid;

    TokenPayload(final String name, final Map<String, String> data, final UserIdentity user, final Set<String> dest, final String guid) {
        this.date = Instant.now();
        this.data = data == null ? Collections.emptyMap() : Collections.unmodifiableMap(data);
        this.name = name;
        this.userIdentity = user;
        this.dest = dest == null ? Collections.emptySet() : Collections.unmodifiableSet(dest);
        this.guid = guid;
    }


    public String toDebugString() {
        final Map<String,String> debugMap = new HashMap<>();
        debugMap.put("date", JavaHelper.toIsoDate(date));
        debugMap.put("name", getName());
        if (getUserIdentity() != null) {
            debugMap.put("user", getUserIdentity().toDisplayString());
        }
        debugMap.put("guid", getGuid());
        return JsonUtil.serializeMap(debugMap);
    }
}

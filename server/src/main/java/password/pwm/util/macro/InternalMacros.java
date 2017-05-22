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

package password.pwm.util.macro;

import password.pwm.PwmApplication;
import password.pwm.PwmConstants;
import password.pwm.PwmEnvironment;
import password.pwm.config.PwmSetting;
import password.pwm.http.ContextManager;
import password.pwm.util.logging.PwmLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class InternalMacros {

    private static final PwmLogger LOGGER = PwmLogger.forClass(InternalMacros.class);

    public static final Map<Class<? extends MacroImplementation>,MacroImplementation.Scope> INTERNAL_MACROS;

    static {
        final Map<Class<? extends MacroImplementation>,MacroImplementation.Scope>  defaultMacros = new HashMap<>();
        defaultMacros.put(PwmSettingReference.class, MacroImplementation.Scope.Static);
        defaultMacros.put(PwmAppName.class, MacroImplementation.Scope.Static);
        defaultMacros.put(PwmContextPath.class, MacroImplementation.Scope.System);
        INTERNAL_MACROS = Collections.unmodifiableMap(defaultMacros);
    }

    abstract static class InternalAbstractMacro extends AbstractMacro {
        @Override
        public MacroDefinitionFlag[] flags() {
            return new MacroDefinitionFlag[] { MacroDefinitionFlag.OnlyDebugLogging };
        }
    }

    public static class PwmSettingReference extends InternalAbstractMacro {
        private static final Pattern PATTERN = Pattern.compile("@PwmSettingReference" + PATTERN_OPTIONAL_PARAMETER_MATCH + "@" );

        public Pattern getRegExPattern() {
            return PATTERN;
        }

        public String replaceValue(final String matchValue, final MacroRequestInfo macroRequestInfo)
                throws MacroParseException
        {
            final String settingKeyStr = matchValue.substring(21, matchValue.length() - 1);
            if (settingKeyStr.isEmpty()) {
                throw new MacroParseException("PwmSettingReference macro requires a setting key value");
            }
            final PwmSetting setting = PwmSetting.forKey(settingKeyStr);
            if (setting == null) {
                throw new MacroParseException("PwmSettingReference macro has unknown key value '" + settingKeyStr + "'");
            }
            return setting.toMenuLocationDebug(null, PwmConstants.DEFAULT_LOCALE);
        }
    }

    public static class PwmContextPath extends InternalAbstractMacro {
        private static final Pattern PATTERN = Pattern.compile("@PwmContextPath@" );

        public Pattern getRegExPattern() {
            return PATTERN;
        }

        public String replaceValue(final String matchValue, final MacroRequestInfo macroRequestInfo)
                throws MacroParseException
        {
            String contextName = "[context]";
            final PwmApplication pwmApplication = macroRequestInfo.getPwmApplication();
            if (pwmApplication != null) {
                final PwmEnvironment pwmEnvironment = pwmApplication.getPwmEnvironment();
                if (pwmEnvironment != null) {
                    final ContextManager contextManager = pwmEnvironment.getContextManager();
                    if (contextManager != null && contextManager.getContextPath() != null) {
                        contextName = contextManager.getContextPath();
                    }
                }
            }
            return contextName;
        }
    }




    public static class PwmAppName extends InternalAbstractMacro {
        private static final Pattern PATTERN = Pattern.compile("@PwmAppName@" );

        public Pattern getRegExPattern() {
            return PATTERN;
        }

        public String replaceValue(final String matchValue, final MacroRequestInfo macroRequestInfo)
                throws MacroParseException
        {
            return PwmConstants.PWM_APP_NAME;
        }
    }
}

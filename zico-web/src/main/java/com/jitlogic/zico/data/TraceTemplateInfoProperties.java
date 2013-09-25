/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zico.data;


import com.google.gwt.editor.client.Editor;
import com.jitlogic.zico.data.TraceTemplateInfo;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.LabelProvider;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;

public interface TraceTemplateInfoProperties extends PropertyAccess<TraceTemplateInfo> {
    @Editor.Path("id")
    ModelKeyProvider<TraceTemplateInfo> key();

    @Editor.Path("description")
    LabelProvider<TraceTemplateInfo> nameLabel();

    ValueProvider<TraceTemplateInfo, Integer> id();

    ValueProvider<TraceTemplateInfo, Integer> traceId();

    ValueProvider<TraceTemplateInfo, Integer> order();

    ValueProvider<TraceTemplateInfo, Integer> flags();

    ValueProvider<TraceTemplateInfo, String> condTemplate();

    ValueProvider<TraceTemplateInfo, String> condRegex();

    ValueProvider<TraceTemplateInfo, String> template();
}

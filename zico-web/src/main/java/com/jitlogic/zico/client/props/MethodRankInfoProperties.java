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
package com.jitlogic.zico.client.props;


import com.google.gwt.editor.client.Editor;
import com.jitlogic.zico.shared.data.MethodRankInfoProxy;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.LabelProvider;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;

public interface MethodRankInfoProperties extends PropertyAccess<MethodRankInfoProxy> {

    @Editor.Path("method")
    ModelKeyProvider<MethodRankInfoProxy> key();

    @Editor.Path("method")
    LabelProvider<MethodRankInfoProxy> label();

    ValueProvider<MethodRankInfoProxy, String> method();

    ValueProvider<MethodRankInfoProxy, Long> calls();

    ValueProvider<MethodRankInfoProxy, Long> errors();

    ValueProvider<MethodRankInfoProxy, Long> time();

    ValueProvider<MethodRankInfoProxy, Long> avgTime();

    ValueProvider<MethodRankInfoProxy, Long> minTime();

    ValueProvider<MethodRankInfoProxy, Long> maxTime();

    ValueProvider<MethodRankInfoProxy, Long> bareTime();

    ValueProvider<MethodRankInfoProxy, Long> avgBareTime();

    ValueProvider<MethodRankInfoProxy, Long> minBareTime();

    ValueProvider<MethodRankInfoProxy, Long> maxBareTime();
}

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
package com.jitlogic.zorka.central.client;


import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface Resources extends ClientBundle {

    public final static Resources INSTANCE = GWT.create(Resources.class);

    @Source("images/error-mark.png")
    ImageResource errorMarkIcon();

    @Source("images/refresh.png")
    ImageResource refreshIcon();

    @Source("images/time.png")
    ImageResource timeIcon();

    @Source("images/filter.png")
    ImageResource filterIcon();

    @Source("images/go-next.png")
    ImageResource goNextIcon();

    @Source("images/go-previous.png")
    ImageResource goPrevIcon();

    @Source("images/go-down-search.png")
    ImageResource goDownIcon();

    @Source("images/exception-thrown.png")
    ImageResource exceptionIcon();

    @Source("images/expand.png")
    ImageResource expandIcon();

    @Source("images/add.png")
    ImageResource addIcon();

    @Source("images/remove.png")
    ImageResource removeIcon();

    @Source("images/edit.png")
    ImageResource editIcon();

    @Source("images/list-columns.png")
    ImageResource listColumnsIcon();
}

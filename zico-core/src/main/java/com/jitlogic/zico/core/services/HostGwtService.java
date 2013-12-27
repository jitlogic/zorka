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
package com.jitlogic.zico.core.services;


import com.jitlogic.zico.core.HostStore;
import com.jitlogic.zico.core.HostStoreManager;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.shared.data.HostProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;


@Singleton
public class HostGwtService {

    private final static Logger log = LoggerFactory.getLogger(HostGwtService.class);

    private HostStoreManager hsm;
    private UserContext ctx;

    @Inject
    public HostGwtService(HostStoreManager hsm, UserContext ctx) {
        this.hsm = hsm;
        this.ctx = ctx;
    }


    public List<HostStore> findAll() {
        return hsm.list(ctx.isInRole("ADMIN") ? null : ctx.getUser());
    }


    public void persist(HostStore host) {
        host.save();
    }

    public void remove(HostStore host) {
        try {
            hsm.delete(host.getName());
        } catch (IOException e) {
            log.error("Cannot remove host", e);
        }
    }

    public void rebuildIndex(HostStore host) {
        try {
            if (!host.hasFlag(HostProxy.CHK_IN_PROGRESS)) {
                host.rebuildIndex();
            }
        } catch (IOException e) {
            log.error("Cannot rebuilt index for host " + host.getName(), e);
        }
    }
}

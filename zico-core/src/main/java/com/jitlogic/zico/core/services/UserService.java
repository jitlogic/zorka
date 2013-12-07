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

import com.google.inject.Singleton;
import com.jitlogic.zico.core.UserContext;
import com.jitlogic.zico.core.ZicoRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Singleton
@Path("users")
public class UserService {

    private UserContext userContext;
    private JdbcTemplate jdbc;

    @Inject
    public UserService(UserContext userContext, DataSource ds) {
        this.userContext = userContext;
        this.jdbc = new JdbcTemplate(ds);
    }

    @GET
    @Path("/self/roles")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> myRoles() {
        return userContext.listRoles();
    }

    @POST
    @Path("/self/password")
    public void resetPassword(
            @QueryParam("userName") String userName,
            @QueryParam("oldPassword") String oldPassword,
            @QueryParam("newPassword") String newPassword) {

        boolean adminMode = userContext.isInRole("ADMIN");

        if (userName != null && !adminMode) {
            throw new ZicoRuntimeException("Insufficient privileges to reset other users password");
        }

        String user = (userName != null && userName.length() > 0) ? userName : userContext.getUser();

        if (!adminMode) {
            String chkHash = "MD5:" + ZorkaUtil.md5(oldPassword);

            String oldHash = jdbc.queryForObject("select PASSWORD from USERS where USER_NAME = ?", String.class, user);

            if (!chkHash.equals(oldHash)) {
                throw new ZicoRuntimeException("Invalid (old) password.");
            }
        }

        jdbc.update("update USERS set PASSWORD = ? where USER_NAME = ?", "MD5:" + ZorkaUtil.md5(newPassword), user);
    }
}

/*-
 * #%L
 * Container JFR
 * %%
 * Copyright (C) 2020 Red Hat, Inc.
 * %%
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * #L%
 */
package com.redhat.rhjmc.containerjfr.net.web.handlers;

import javax.inject.Inject;

import com.redhat.rhjmc.containerjfr.core.log.Logger;
import com.redhat.rhjmc.containerjfr.core.templates.TemplateType;
import com.redhat.rhjmc.containerjfr.net.AuthManager;
import com.redhat.rhjmc.containerjfr.net.TargetConnectionManager;
import com.redhat.rhjmc.containerjfr.net.web.HttpMimeType;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.HttpStatusException;

class TargetTemplateGetHandler extends AbstractAuthenticatedRequestHandler {

    private final TargetConnectionManager targetConnectionManager;
    private final Logger logger;

    @Inject
    TargetTemplateGetHandler(
            AuthManager auth, TargetConnectionManager targetConnectionManager, Logger logger) {
        super(auth);
        this.targetConnectionManager = targetConnectionManager;
        this.logger = logger;
    }

    @Override
    public HttpMethod httpMethod() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/api/v1/targets/:targetId/templates/:templateName/type/:templateType";
    }

    @Override
    void handleAuthenticated(RoutingContext ctx) {
        String templateName = ctx.pathParam("templateName");
        TemplateType templateType = TemplateType.valueOf(ctx.pathParam("templateType"));
        try {
            targetConnectionManager
                    .executeConnectedTask(
                            getConnectionDescriptorFromContext(ctx),
                            conn -> conn.getTemplateService().getXml(templateName, templateType))
                    .ifPresentOrElse(
                            doc -> {
                                ctx.response()
                                        .putHeader(
                                                HttpHeaders.CONTENT_TYPE, HttpMimeType.JFC.mime());
                                ctx.response().end(doc.toString());
                            },
                            () -> {
                                throw new HttpStatusException(404);
                            });
        } catch (HttpStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.warn(e);
            throw new HttpStatusException(500, e);
        }
    }
}

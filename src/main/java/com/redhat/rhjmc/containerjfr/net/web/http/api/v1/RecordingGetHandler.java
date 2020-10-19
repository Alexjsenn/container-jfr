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
package com.redhat.rhjmc.containerjfr.net.web.http.api.v1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import com.redhat.rhjmc.containerjfr.MainModule;
import com.redhat.rhjmc.containerjfr.core.log.Logger;
import com.redhat.rhjmc.containerjfr.core.sys.Environment;
import com.redhat.rhjmc.containerjfr.net.AuthManager;
import com.redhat.rhjmc.containerjfr.net.ConnectionDescriptor;
import com.redhat.rhjmc.containerjfr.net.web.WebServer.DownloadDescriptor;
import com.redhat.rhjmc.containerjfr.net.web.http.api.ApiVersion;
import io.vertx.ext.web.RoutingContext;

class RecordingGetHandler extends TargetRecordingGetHandler {

    private final Path savedRecordingsPath;

    @Inject
    RecordingGetHandler(
            AuthManager auth,
            Environment env,
            @Named(MainModule.RECORDINGS_PATH) Path savedRecordingsPath,
            Logger logger) {
        super(auth, env, null, logger);
        this.savedRecordingsPath = savedRecordingsPath;
        if (env.hasEnv(USE_LOW_MEM_PRESSURE_STREAMING_ENV)) {
            logger.info("low memory pressure streaming enabled for web server");
        } else {
            logger.info("low memory pressure streaming disabled for web server");
        }
    }

    @Override
    public ApiVersion apiVersion() {
        return ApiVersion.V1;
    }

    @Override
    public String path() {
        return basePath() + "recordings/:recordingName";
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isOrdered() {
        return false;
    }

    @Override
    public void handleAuthenticated(RoutingContext ctx) throws Exception {
        String recordingName = ctx.pathParam("recordingName");
        handleRecordingDownloadRequest(ctx, recordingName);
    }

    @Override
    Optional<DownloadDescriptor> getRecordingDescriptor(
            ConnectionDescriptor unused, String recordingName) {
        try {
            // TODO refactor Files calls into FileSystem for testability
            Optional<Path> savedRecording =
                    Files.list(savedRecordingsPath)
                            .filter(
                                    saved ->
                                            saved.getFileName()
                                                    .toFile()
                                                    .getName()
                                                    .equals(recordingName))
                            .findFirst();
            if (savedRecording.isPresent()) {
                return Optional.of(
                        new DownloadDescriptor(
                                Files.newInputStream(savedRecording.get(), StandardOpenOption.READ),
                                Files.size(savedRecording.get()),
                                null));
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return Optional.empty();
    }
}
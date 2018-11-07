/*
 *
 *  SecureCodeBox (SCB)
 *  Copyright 2015-2018 iteratec GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package io.securecodebox.engine.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.securecodebox.engine.service.ProcessService;
import io.securecodebox.model.securitytest.SecurityTestConfiguration;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

@Api(value = "securityTests",
        description = "Manage securityTests.",
        produces = "application/json",
        consumes = "application/json")
@RestController
@RequestMapping(value = "/box/securityTests")
public class SecurityTestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityTestResource.class);

    @Autowired
    ProcessService processService;

    @Autowired
    ObjectMapper objectMapper;

    @ApiOperation(value = "Starts new securityTests.",
                    notes = "Starts new securityTests, based on a given list of securityTest configurations.",
                    authorizations = {
                            @Authorization(value="basicAuth")
                    }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    code = 201,
                    message = "Successful created a new securityTest returns the process id.",
                    response = UUID.class,
                    responseContainer = "List"
            ),
            @ApiResponse(
                    code = 300,
                    message = "For some reason multiple securityTest definitions could be addressed by the given securityTest name.",
                    response = void.class
            ),
            @ApiResponse(
                    code = 400,
                    message = "Incomplete or inconsistent Request."
            ),
            @ApiResponse(
                    code = 401,
                    message = "Unauthenticated",
                    response = void.class
            ),
            @ApiResponse(
                    code = 404,
                    message = "Could not find definition for specified securityTest.",
                    response = void.class
            ),
            @ApiResponse(
                    code = 500,
                    message = "Unknown technical error occurred."
            )
    })
    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<List<UUID>> startSecurityTests(
            @Valid
            @RequestBody
            @ApiParam(
                value = "A list with all securityTest which should be performed.",
                required = true
            )
            List<SecurityTestConfiguration> securityTests
    ) {

        for (SecurityTestConfiguration securityTest : securityTests) {
            try {
                this.processService.checkProcessExistence(securityTest.getProcessDefinitionKey());
            } catch (ProcessService.NonExistentProcessException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } catch (ProcessService.DuplicateProcessDefinitionForKeyException e) {
                return ResponseEntity.status(HttpStatus.MULTIPLE_CHOICES).build();
            }
        }

        List<UUID> processInstances = new LinkedList<>();

        for (SecurityTestConfiguration securityTest : securityTests) {
            processInstances.add(processService.startProcess(securityTest));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(processInstances);
    }
}

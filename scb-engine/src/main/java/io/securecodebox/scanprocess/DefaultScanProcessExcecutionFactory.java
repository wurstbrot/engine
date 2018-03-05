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

package io.securecodebox.scanprocess;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Rüdiger Heins - iteratec GmbH
 * @since 01.03.18
 */
@Component
public class DefaultScanProcessExcecutionFactory implements ScanProcessExecutionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultScanProcessExcecutionFactory.class);

    @Override
    public ScanProcessExecution get(DelegateExecution execution) {
        return get(execution, DefaultScanProcessExcecution.class);
    }

    @Override
    public <P extends ScanProcessExecution> P get(DelegateExecution execution, Class<P> customProcess) {

        try {
            return customProcess.getConstructor(DelegateExecution.class).newInstance(execution);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOG.error("Error creating custom process execution", e);
            throw new IllegalStateException("Error creating custom process execution", e);
        }
    }
}

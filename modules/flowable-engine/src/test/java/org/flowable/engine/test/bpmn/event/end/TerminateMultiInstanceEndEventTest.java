/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.bpmn.event.end;

import java.util.List;

import org.flowable.engine.common.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.service.Job;

/**
 * @author Joram Barrez
 */
public class TerminateMultiInstanceEndEventTest extends PluggableFlowableTestCase {

    @Deployment
    public void testMultiInstanceEmbeddedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateMi");

        org.flowable.task.service.Task aTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(aTask.getId());

        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(8, bTasks.size());

        // Complete 2 tasks by going to task C. The 3th tasks goes to the MI terminate end and shuts down the MI.
        for (int i = 0; i < 2; i++) {
            org.flowable.task.service.Task bTask = bTasks.get(i);
            taskService.complete(bTask.getId(), CollectionUtil.singletonMap("myVar", "toC"));
        }

        bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(6, bTasks.size());

        taskService.complete(bTasks.get(0).getId(), CollectionUtil.singletonMap("myVar", "toEnd"));

        org.flowable.task.service.Task afterMiTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("AfterMi", afterMiTask.getName());
        taskService.complete(afterMiTask.getId());

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment
    public void testMultiInstanceEmbeddedSubprocessSequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateMi");

        org.flowable.task.service.Task aTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(aTask.getId());

        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, bTasks.size());
        taskService.complete(bTasks.get(0).getId(), CollectionUtil.singletonMap("myVar", "toC"));

        List<org.flowable.task.service.Task> cTasks = taskService.createTaskQuery().taskName("C").list();
        assertEquals(1, cTasks.size());
        taskService.complete(cTasks.get(0).getId());

        bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(1, bTasks.size());
        taskService.complete(bTasks.get(0).getId(), CollectionUtil.singletonMap("myVar", "toEnd"));

        org.flowable.task.service.Task afterMiTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("AfterMi", afterMiTask.getName());
        taskService.complete(afterMiTask.getId());

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment
    public void testMultiInstanceEmbeddedSubprocess2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateMi");

        org.flowable.task.service.Task aTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(aTask.getId());

        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(5, bTasks.size());

        // Complete one b task to get one C and D
        taskService.complete(bTasks.get(0).getId());

        // C and D should now be active
        List<org.flowable.task.service.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(6, tasks.size());
        // 0-3 are B tasks
        assertEquals("C", tasks.get(4).getName());
        assertEquals("D", tasks.get(5).getName());

        // Completing C should terminate the multi instance
        taskService.complete(tasks.get(4).getId());

        org.flowable.task.service.Task afterMiTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("AfterMi", afterMiTask.getName());
        taskService.complete(afterMiTask.getId());

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment
    public void testMultiInstanceEmbeddedSubprocess2Sequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateMi");

        org.flowable.task.service.Task aTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(aTask.getId());

        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, bTasks.size());

        // Complete one b task to get one C and D
        taskService.complete(bTasks.get(0).getId());

        // C and D should now be active
        List<org.flowable.task.service.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("C", tasks.get(0).getName());
        assertEquals("D", tasks.get(1).getName());

        // Completing C should terminate the multi instance
        taskService.complete(tasks.get(0).getId());

        org.flowable.task.service.Task afterMiTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("AfterMi", afterMiTask.getName());
        taskService.complete(afterMiTask.getId());

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateMultiInstanceEndEventTest.testTerminateMiCallactivity-parentProcess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/end/TerminateMultiInstanceEndEventTest.testTerminateMiCallactivity-calledProcess.bpmn20.xml"
    })
    public void testTerminateMiCallactivity() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateMiCallActivity");

        org.flowable.task.service.Task taskA = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("A", taskA.getName());
        taskService.complete(taskA.getId());

        // After completing A, four B's should be active (due to the call activity)
        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().taskName("B").list();
        assertEquals(4, bTasks.size());

        // Completing 3 B tasks, giving 3 C's and D's
        for (int i = 0; i < 3; i++) {
            taskService.complete(bTasks.get(i).getId());
        }

        List<org.flowable.task.service.Task> cTasks = taskService.createTaskQuery().taskName("C").list();
        assertEquals(3, cTasks.size());
        List<org.flowable.task.service.Task> dTasks = taskService.createTaskQuery().taskName("D").list();
        assertEquals(3, dTasks.size());

        // Completing one of the C tasks should terminate the whole multi instance
        taskService.complete(cTasks.get(0).getId());

        List<org.flowable.task.service.Task> afterMiTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, afterMiTasks.size());
        assertEquals("AfterMi", afterMiTasks.get(0).getName());
        assertEquals("Parallel task", afterMiTasks.get(1).getName());
    }

    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/end/TerminateMultiInstanceEndEventTest.testTerminateMiCallactivity-parentProcessSequential.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/end/TerminateMultiInstanceEndEventTest.testTerminateMiCallactivity-calledProcess.bpmn20.xml"
    })
    public void testTerminateMiCallactivitySequential() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("terminateMiCallActivity");

        org.flowable.task.service.Task taskA = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("A", taskA.getName());
        taskService.complete(taskA.getId());

        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().taskName("B").list();
        assertEquals(1, bTasks.size());
        taskService.complete(bTasks.get(0).getId());

        List<org.flowable.task.service.Task> cTasks = taskService.createTaskQuery().taskName("C").list();
        assertEquals(1, cTasks.size());
        List<org.flowable.task.service.Task> dTasks = taskService.createTaskQuery().taskName("D").list();
        assertEquals(1, dTasks.size());

        // Completing one of the C tasks should terminate the whole multi instance
        taskService.complete(cTasks.get(0).getId());

        List<org.flowable.task.service.Task> afterMiTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, afterMiTasks.size());
        assertEquals("AfterMi", afterMiTasks.get(0).getName());
        assertEquals("Parallel task", afterMiTasks.get(1).getName());
    }

    @Deployment
    public void testTerminateNestedMiEmbeddedSubprocess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "terminateNestedMiEmbeddedSubprocess", CollectionUtil.singletonMap("var", "notEnd"));

        List<org.flowable.task.service.Task> aTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("A").list();
        assertEquals(12, aTasks.size());
        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(72, bTasks.size());

        // Completing a few B's will create a subprocess with some C's
        int nrOfBTasksCompleted = 3;
        for (int i = 0; i < nrOfBTasksCompleted; i++) {
            taskService.complete(bTasks.get(i).getId());
        }

        bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(72 - nrOfBTasksCompleted, bTasks.size());

        // Firing the timer --> inner MI gets destroyed
        List<Job> timers = managementService.createTimerJobQuery().list();
        assertEquals(nrOfBTasksCompleted, timers.size());
        managementService.moveTimerToExecutableJob(timers.get(0).getId());
        managementService.executeJob(timers.get(0).getId());

        // We only completed 3 B's. 3 other ones should be destroyed too (as one inner multi instance are 6 instances of B)
        bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(66, bTasks.size());

        // One of the inner multi instances should have been killed
        List<org.flowable.task.service.Task> afterInnerMiTasks = taskService.createTaskQuery().taskName("AfterInnerMi").list();
        assertEquals(1, afterInnerMiTasks.size());

        for (org.flowable.task.service.Task aTask : aTasks) {
            taskService.complete(aTask.getId());
        }

        // Finish
        List<org.flowable.task.service.Task> nextTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        while (nextTasks != null && nextTasks.size() > 0) {
            taskService.complete(nextTasks.get(0).getId());
            nextTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        }

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Deployment(resources = "org/flowable/engine/test/bpmn/event/end/TerminateMultiInstanceEndEventTest.testTerminateNestedMiEmbeddedSubprocess.bpmn20.xml")
    public void testTerminateNestedMiEmbeddedSubprocess2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "terminateNestedMiEmbeddedSubprocess", CollectionUtil.singletonMap("var", "toEnd"));

        List<org.flowable.task.service.Task> aTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("A").list();
        assertEquals(12, aTasks.size());
        List<org.flowable.task.service.Task> afterInnerMiTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("AfterInnerMi").list();
        assertEquals(12, afterInnerMiTasks.size());

    }

    @Deployment
    public void testTerminateNestedMiEmbeddedSubprocessWithOneLoopCardinality() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "terminateNestedMiEmbeddedSubprocess", CollectionUtil.singletonMap("var", "notEnd"));

        List<org.flowable.task.service.Task> aTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("A").list();
        assertEquals(1, aTasks.size());
        List<org.flowable.task.service.Task> bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(1, bTasks.size());

        taskService.complete(bTasks.get(0).getId());
        bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(0, bTasks.size());

        // Firing the timer --> inner MI gets destroyed
        List<Job> timers = managementService.createTimerJobQuery().list();
        assertEquals(1, timers.size());
        managementService.moveTimerToExecutableJob(timers.get(0).getId());
        managementService.executeJob(timers.get(0).getId());

        bTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskName("B").list();
        assertEquals(0, bTasks.size());

        // One of the inner multi instances should have been killed
        List<org.flowable.task.service.Task> afterInnerMiTasks = taskService.createTaskQuery().taskName("AfterInnerMi").list();
        assertEquals(1, afterInnerMiTasks.size());

        for (org.flowable.task.service.Task aTask : aTasks) {
            taskService.complete(aTask.getId());
        }

        // Finish
        List<org.flowable.task.service.Task> nextTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        while (nextTasks != null && nextTasks.size() > 0) {
            taskService.complete(nextTasks.get(0).getId());
            nextTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        }

        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

}

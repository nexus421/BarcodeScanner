package bayern.kickner.barcode_scanner_library

import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * [Task.await] is what lets [BarcodeScannerDialogV3]'s scan loop suspend for one frame's ML Kit decode instead of
 * registering fire-and-forget listeners, giving the loop backpressure. These tests lock in all three outcomes a
 * Google Play Services [Task] can report - including cancellation, which used to be silently ignored (the coroutine
 * would just hang forever) before [Task.addOnCanceledListener] was wired up.
 */
class TaskAwaitTest {

    @Suppress("UNCHECKED_CAST")
    private fun mockTask(): Task<String> = mock(Task::class.java) as Task<String>

    @Test
    fun `successful task resumes with its result`() {
        val task = mockTask()
        `when`(task.addOnSuccessListener(any())).thenAnswer { invocation ->
            invocation.getArgument<OnSuccessListener<String>>(0).onSuccess("barcode-value")
            task
        }
        `when`(task.addOnFailureListener(any())).thenReturn(task)
        `when`(task.addOnCanceledListener(any())).thenReturn(task)

        val result = runBlocking { task.await() }

        assertEquals("barcode-value", result)
    }

    @Test(expected = IllegalStateException::class)
    fun `failed task resumes with the original exception, not a wrapped one`() {
        val task = mockTask()
        val failure = IllegalStateException("boom")
        `when`(task.addOnSuccessListener(any())).thenReturn(task)
        `when`(task.addOnFailureListener(any())).thenAnswer { invocation ->
            invocation.getArgument<OnFailureListener>(0).onFailure(failure)
            task
        }
        `when`(task.addOnCanceledListener(any())).thenReturn(task)

        runBlocking { task.await() }
    }

    @Test(expected = CancellationException::class)
    fun `canceled task cancels the coroutine instead of hanging forever`() {
        val task = mockTask()
        `when`(task.addOnSuccessListener(any())).thenReturn(task)
        `when`(task.addOnFailureListener(any())).thenReturn(task)
        `when`(task.addOnCanceledListener(any())).thenAnswer { invocation ->
            invocation.getArgument<OnCanceledListener>(0).onCanceled()
            task
        }

        runBlocking { task.await() }
    }
}

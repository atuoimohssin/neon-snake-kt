package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.snake.model.Dir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Snake Game", appName)
  }

  @Test
  fun `verify opposite directions`() {
    assertTrue(Dir.UP.isOpposite(Dir.DOWN))
    assertTrue(Dir.LEFT.isOpposite(Dir.RIGHT))
  }
}

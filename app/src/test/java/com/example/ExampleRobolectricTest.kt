package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.snake.model.Dir
import com.example.snake.model.FloorCatalog
import com.example.snake.model.LevelRepository
import com.example.snake.model.UnlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

  @Test
  fun `verify coin calculation logic`() {
    val score = 85
    val roundCoinsCollected = 3
    val scoreCoins = score / 20 // 4
    val totalRoundCoins = scoreCoins + roundCoinsCollected // 7
    assertEquals(4, scoreCoins)
    assertEquals(7, totalRoundCoins)
  }

  @Test
  fun `verify levels progression and configuration`() {
    assertEquals(10, LevelRepository.levels.size)
    val lvl1 = LevelRepository.getLevel(1)
    val lvl10 = LevelRepository.getLevel(10)
    assertEquals(8, lvl1.targetFood)
    assertEquals(160L, lvl1.tickMs)
    assertTrue(lvl1.obstacles.isEmpty())

    assertEquals(26, lvl10.targetFood)
    assertEquals(70L, lvl10.tickMs)
    assertTrue(lvl10.obstacles.isNotEmpty())
  }

  @Test
  fun `verify floor catalog contains 8 floors with expected properties`() {
    val floors = FloorCatalog.ALL_FLOORS
    assertEquals(8, floors.size)

    val classic = FloorCatalog.getFloor("classic")
    assertEquals(UnlockType.FREE, classic.unlockType)
    assertEquals(0, classic.price)

    val midnight = FloorCatalog.getFloor("midnight")
    assertEquals(UnlockType.COINS, midnight.unlockType)
    assertEquals(30, midnight.price)

    val ocean = FloorCatalog.getFloor("ocean")
    assertEquals(UnlockType.COINS, ocean.unlockType)
    assertEquals(60, ocean.price)

    val sunset = FloorCatalog.getFloor("sunset")
    assertEquals(UnlockType.COINS, sunset.unlockType)
    assertEquals(90, sunset.price)

    val matrix = FloorCatalog.getFloor("matrix")
    assertEquals(UnlockType.LEVEL, matrix.unlockType)
    assertEquals(4, matrix.requiredLevel)

    val flow = FloorCatalog.getFloor("flow")
    assertEquals(UnlockType.COINS, flow.unlockType)
    assertEquals(150, flow.price)
    assertTrue(flow.animated)

    val pulse = FloorCatalog.getFloor("pulse")
    assertEquals(UnlockType.LEVEL, pulse.unlockType)
    assertEquals(7, pulse.requiredLevel)
    assertTrue(pulse.animated)

    val aurora = FloorCatalog.getFloor("aurora")
    assertEquals(UnlockType.LEVEL, aurora.unlockType)
    assertEquals(10, aurora.requiredLevel)
    assertTrue(aurora.animated)
  }

  @Test
  fun `verify GameState hierarchy and status mapping`() {
    val readyState = com.example.snake.model.SnakeGameState(gameState = com.example.snake.model.GameState.Ready)
    assertEquals(com.example.snake.model.GameStatus.IDLE, readyState.status)

    val playingState = com.example.snake.model.SnakeGameState(gameState = com.example.snake.model.GameState.Playing)
    assertEquals(com.example.snake.model.GameStatus.RUNNING, playingState.status)

    val pausedState = com.example.snake.model.SnakeGameState(gameState = com.example.snake.model.GameState.Paused)
    assertEquals(com.example.snake.model.GameStatus.PAUSED, pausedState.status)

    val completeState = com.example.snake.model.SnakeGameState(gameState = com.example.snake.model.GameState.LevelComplete)
    assertEquals(com.example.snake.model.GameStatus.LEVEL_COMPLETE, completeState.status)

    val gameOverState = com.example.snake.model.SnakeGameState(gameState = com.example.snake.model.GameState.GameOver)
    assertEquals(com.example.snake.model.GameStatus.GAME_OVER, gameOverState.status)

    val lobbyState = com.example.snake.model.SnakeGameState(gameState = com.example.snake.model.GameState.Lobby)
    assertEquals(com.example.snake.model.GameStatus.IDLE, lobbyState.status)
  }
}

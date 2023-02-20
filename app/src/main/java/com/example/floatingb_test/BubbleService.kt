package com.example.floatingb_test

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BADGE_ICON_NONE
import androidx.core.app.NotificationCompat.VISIBILITY_SECRET
import kotlin.math.abs
import kotlin.math.roundToInt


class BubbleService : Service() {
    companion object {
        const val ONGOING_NOTIFICATION_ID = 256
        const val ACTION_STOP = "service_stop"
    }

    private lateinit var bubbleView: View
    private lateinit var bubbleLayoutParams: WindowManager.LayoutParams
    private lateinit var windowManager: WindowManager
    private lateinit var mainActivityIntent: Intent
    private lateinit var pendingIntent: PendingIntent
    private lateinit var notification: Notification

    var endPosition: Int = 0
    var startPosition: Int = 0

    private lateinit var gestureDetector: GestureDetector
    private val stickAnimationDuration = 300L


    val swipesMap = mutableMapOf<SwipeDirections, Boolean>(
        SwipeDirections.LEFT to false,
        SwipeDirections.RIGHT to false,
        SwipeDirections.UP to false,
        SwipeDirections.DOWN to false
    )

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if ((intent?.action) == ACTION_STOP) {
            stopSelf()
        }
        val stopSelf = Intent(this, BubbleService::class.java)
        stopSelf.action = ACTION_STOP
        mainActivityIntent = Intent(this, MainActivity::class.java)

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pendingIntent =
            PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationManager =
            this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannelGroup(
                NotificationChannelGroup(
                    getString(R.string.bubbles_group),
                    getString(R.string.bubbles)
                )
            )

            val notificationChannel =
                NotificationChannel(
                    getString(R.string.service_channel),
                    getString(R.string.service_notifications),
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    enableLights(false)
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                }
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(this, getString(R.string.service_channel))
            .setContentTitle("${resources.getString(R.string.app_name)} ${resources.getString(R.string.service_is_running)}")
            .setContentText(getString(R.string.touch_to_open_app))
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.dta_logo))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setVisibility(VISIBILITY_SECRET)
            .setBadgeIconType(BADGE_ICON_NONE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.stop),
                stopPendingIntent
            )
            .setOngoing(true)

        notification = builder.build()
        startForeground(ONGOING_NOTIFICATION_ID, notification)
        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()

        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)

        bubbleLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager


        val screenWidth = GetDisplayMetrics.getScreenSize(applicationContext).width
        startPosition =
            ((-screenWidth / 2) + resources.getDimension(R.dimen.bubble_size_px)).roundToInt()
        endPosition =
            ((screenWidth / 2) - resources.getDimension(R.dimen.bubble_size_px)).roundToInt()
        // Set the initial position of the bubble
        bubbleLayoutParams.x = screenWidth / 2 - 70
        bubbleLayoutParams.y = 0

        windowManager.addView(bubbleView, bubbleLayoutParams)

        gestureDetector =
            GestureDetector(applicationContext, object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD: Int = 50
                private val SWIPE_VELOCITY_THRESHOLD: Int = 50

                override fun onLongPress(e: MotionEvent) {
                    stopSelf()
                    Log.d(applicationContext.packageName, "Long press is detected!")
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    Log.d(applicationContext.packageName, "Click is detected!")
                    mainActivityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    applicationContext.startActivity(mainActivityIntent)
                    return super.onSingleTapUp(e)
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val i = Intent(Intent.ACTION_MAIN)
                    i.addCategory(Intent.CATEGORY_HOME)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(i)
                    Log.d(applicationContext.packageName, "Double tap is detected!")
                    return super.onDoubleTapEvent(e)
                }

                override fun onFling(
                    even1: MotionEvent,
                    event2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    try {
                        val diffY = event2.y - even1.y
                        val diffX = event2.x - even1.x
                        if (abs(diffX) > abs(diffY)) {
                            if (abs(diffX) > SWIPE_THRESHOLD && abs(
                                    velocityX
                                ) > SWIPE_VELOCITY_THRESHOLD
                            ) {
                                if (diffX > 0) swipesMap[SwipeDirections.RIGHT] =
                                    true else swipesMap[SwipeDirections.LEFT] = true
                            }
                        } else {
                            if (abs(diffY) > SWIPE_THRESHOLD && abs(
                                    velocityY
                                ) > SWIPE_VELOCITY_THRESHOLD
                            ) {
                                if (diffY < 0) swipesMap[SwipeDirections.UP] =
                                    true else swipesMap[SwipeDirections.DOWN] = true
                            }
                        }
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                    return false
                }

            })

        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0
            private var initialTouchY = 0

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        windowManager.updateViewLayout(bubbleView, bubbleLayoutParams)
                        initialX = bubbleLayoutParams.x
                        initialY = bubbleLayoutParams.y
                        initialTouchX = event.rawX.toInt()
                        initialTouchY = event.rawY.toInt()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        bubbleLayoutParams.x = initialX + (event.rawX.toInt() - initialTouchX)
                        bubbleLayoutParams.y = initialY + (event.rawY.toInt() - initialTouchY)
                        windowManager.updateViewLayout(bubbleView, bubbleLayoutParams)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        swipesMap.keys.forEach {
                            if (swipesMap[it] == true) stickToEdge(it)
                        }
                    }
                }
                return false
            }
        })

    }

    fun stickToEdge(direction: SwipeDirections) {
        when (direction) {
            SwipeDirections.LEFT -> {
                val animator =
                    ValueAnimator.ofInt(bubbleLayoutParams.x, startPosition.toInt())
                animator.duration = stickAnimationDuration
                animator.addUpdateListener { animation ->
                    bubbleLayoutParams.x = (animation.animatedValue as Int)
                    windowManager.updateViewLayout(bubbleView, bubbleLayoutParams)
                }
                animator.start()
                swipesMap[direction] = false
            }

            SwipeDirections.RIGHT -> {
                val animator =
                    ValueAnimator.ofInt(bubbleLayoutParams.x, endPosition.toInt())
                animator.duration = stickAnimationDuration
                animator.addUpdateListener { animation ->
                    bubbleLayoutParams.x = (animation.animatedValue as Int)
                    windowManager.updateViewLayout(bubbleView, bubbleLayoutParams)
                }
                animator.start()
                swipesMap[direction] = false
            }

            SwipeDirections.UP -> {
                swipesMap[direction] = false
                Log.d(application.packageName, "swiped up")
            }

            SwipeDirections.DOWN -> {
                swipesMap[direction] = false
                Log.d(application.packageName, "down")
            }

            else -> {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(bubbleView)
    }
}

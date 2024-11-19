package com.example.test1.ui.home

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.test1.R
import com.example.test1.databinding.FragmentHomeBinding
import com.example.test1.utils.ReadJSONFromAssets
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList
import java.util.Queue
import java.util.Timer
import java.util.TimerTask

const val OBJECT_SLIDE_PERIOD_MS    = 500
const val CONF_THRESH               = 0.7
const val HALF_BEAT_MIN_MS          = 250.0
const val BEAT_MIN_MS               = 600.0
const val HALF_BEAT_MAX_MS          = 425.0
const val BEAT_MAX_MS               = 800.0
const val BEAT_MS                   = 750.0
const val HALF_BEAT_MS              = 375.0
const val TIMER_INIT_DELAY          = 0
const val MAX_CLICK_DELAY_MS        = 100

enum class EventType {
    DRUM1, DRUM2, CLAP
}
data class Event(val evntType: EventType, val ts: Double)

class HomeFragment : Fragment() {
    private var beatIdx = 0
    private var segIdx = 0
    private val evntQueue: Queue<Event> = LinkedList() // Queue of launched events waiting for click
    private var nextBeat = 0.0
    private var nextSeg = 0.0
    private var currBeat = 0.0
    private lateinit var nextSegType: EventType
    private lateinit var beatsArray : JSONArray
    private lateinit var segmentsArray : JSONArray
    private var time_ms = 0.0
    private var timer_beats = Timer()
    private var timer_segs = Timer()
    private var timer_evnts = Timer()
    private var songPlaying = false
    private var isCurrClicked = false
    private var _binding: FragmentHomeBinding? = null
    private lateinit var music: MediaPlayer

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.start.setOnClickListener {
            controlSong()
        }
        binding.drum1.setOnClickListener{
            clickEvnt(EventType.DRUM1)
        }
        binding.drum2.setOnClickListener{
            clickEvnt(EventType.DRUM2)
        }
        binding.clap.setOnClickListener{
            clickEvnt(EventType.CLAP)
        }
        // Init music player
//        music = MediaPlayer.create(this.context, R.raw.rockyou);

        // Read song data and initialize data structures
        val jsonString = ReadJSONFromAssets(this.context, "data.json")
        val jsonObj = JSONObject(jsonString)
        beatsArray = jsonObj.getJSONArray("beats")
        segmentsArray = jsonObj.getJSONArray("segments")
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private inner class TimeTaskBeats: TimerTask()
    {
        override fun run()
        {
            if(songPlaying)
            {
                time_ms = music.currentPosition.toDouble() + OBJECT_SLIDE_PERIOD_MS
                // Time for next beat?
                if(time_ms >=nextBeat)
                {
                    // Launch drum1 animation
                    animateDrum1()
                    currBeat = nextBeat
                    // Skip one beat
                    ++beatIdx
                    nextBeat = beatsArray.getJSONObject(++beatIdx).getDouble("start") * 1000
                    evntQueue.add(Event(EventType.DRUM1, currBeat))
                    binding.progressBar.progress = ((music.currentPosition.toFloat()/music.duration.toFloat())*100).toInt()
                }
            }
        }
    }
    private inner class TimeTaskSegments: TimerTask()
    {
        override fun run()
        {
            if(songPlaying)
            {
                // Time for next segment?
                if(time_ms >= nextSeg)
                {
                    var minTimeAdjust = 0.0
                    var maxTimeAdjust = 0.0
                    var timeUnit = 0.0
                    var timeRef = 0.0
                    when(nextSegType){
                        EventType.DRUM2 ->
                        {
                            // Launch drum2 animation
                            animateDrum2()
                            // Parameters for finding next clap in segments array
                            minTimeAdjust = BEAT_MIN_MS
                            maxTimeAdjust = BEAT_MAX_MS
                            timeUnit = BEAT_MS
                            timeRef = currBeat
                            nextSegType = EventType.CLAP
                            // Enqueue event
                            evntQueue.add(Event(EventType.DRUM2, nextSeg))
                        }
                        EventType.CLAP ->
                        {
                            // Launch clap animation
                            animateClap()
                            // Parameters for finding next drum in segments array
                            minTimeAdjust = HALF_BEAT_MIN_MS
                            maxTimeAdjust = HALF_BEAT_MAX_MS
                            timeUnit = HALF_BEAT_MS
                            timeRef = nextBeat
                            nextSegType = EventType.DRUM2
                            // Enqueue event
                            evntQueue.add(Event(EventType.CLAP, nextSeg))
                        }
                        else -> {
                            // Do nothing
                        }
                    }

                    // Find next segment
                    while(segIdx<segmentsArray.length())
                    {
                        val segment = segmentsArray.getJSONObject(segIdx++)
                        if(segment.getDouble("start") * 1000 > timeRef + maxTimeAdjust)
                        {
                            nextSeg = timeRef + timeUnit
                            break
                        }
                        else if(segment.getDouble("confidence")>=CONF_THRESH && (segment.getDouble("start") * 1000 >= timeRef + minTimeAdjust))
                        {
                            // Save segment
                            nextSeg = segment.getDouble("start") * 1000
                            break
                        }
                    }
                }
            }
        }
    }
    private inner class TimeTaskEvnts: TimerTask()
    {
        override fun run()
        {
            if(songPlaying)
            {

                // Next event expired?
                val evnt = evntQueue.peek()

                if (evnt != null) {
                    if(time_ms - OBJECT_SLIDE_PERIOD_MS - MAX_CLICK_DELAY_MS > evnt.ts)
                    {
                        evntQueue.remove()
                        binding.textHome.text = "Raté"
                    }
                }
            }
        }
    }
    fun animateDrum1() {
        val animationSlideDown = AnimationUtils.loadAnimation(this.context, R.anim.slide_down)
        animationSlideDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationRepeat(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationEnd(p0: Animation?) {
                binding.circle.visibility = View.INVISIBLE
            }
        })
        binding.circle.startAnimation(animationSlideDown)
    }
    fun animateDrum2() {
        val animationSlideDown = AnimationUtils.loadAnimation(this.context, R.anim.slide_down)
        animationSlideDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationRepeat(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationEnd(p0: Animation?) {
                binding.circle2.visibility = View.INVISIBLE
            }
        })
        binding.circle2.startAnimation(animationSlideDown)
    }
    fun animateClap() {
        val animationSlideDown = AnimationUtils.loadAnimation(this.context, R.anim.slide_down)
        animationSlideDown.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationRepeat(p0: Animation?) {
//                not implemented
            }

            override fun onAnimationEnd(p0: Animation?) {
                binding.circleClap.visibility = View.INVISIBLE
            }
        })
        binding.circleClap.startAnimation(animationSlideDown)
    }
    private fun controlSong() {
        if(songPlaying)
        {
            // Stop song
            music.stop()
            music.reset()
            timer_beats.cancel()
            timer_segs.cancel()
            timer_evnts.cancel()
            timer_beats.purge()
            timer_segs.purge()
            timer_evnts.purge()
            timer_beats = Timer()
            timer_segs = Timer()
            timer_evnts = Timer()
            songPlaying = false
            isCurrClicked = false
            binding.start.text = "Démarrer"

            beatIdx = 0
            segIdx = 0
            nextBeat = 0.0
            nextSeg = 0.0
            currBeat = 0.0
            time_ms = 0.0
            binding.progressBar.progress = 0
        }
        else
        {
            initEvents()
            // Start song
            music = MediaPlayer.create(this.context, R.raw.rockyou);
            music.start()
            // Setup timers
            timer_beats.schedule(TimeTaskBeats(), TIMER_INIT_DELAY.toLong(), 10)
            timer_segs.schedule(TimeTaskSegments(), TIMER_INIT_DELAY.toLong(), 15)
            timer_evnts.schedule(TimeTaskEvnts(), TIMER_INIT_DELAY.toLong(), 20)
            songPlaying = true
            binding.start.text = "Arrêter"
        }
    }
    private fun clickEvnt(evntType: EventType) {
        val evnt = evntQueue.peek()
        // Click on right icon?
        if (evnt != null) {
            if(evnt.evntType == evntType)
            {
                // Click is timed correctly?
                val timestamp = time_ms - OBJECT_SLIDE_PERIOD_MS
                if(timestamp<evnt.ts+MAX_CLICK_DELAY_MS && timestamp>evnt.ts-50)
                {
                    binding.textHome.text = "Réussi"
                    evntQueue.remove()
                }
                else
                {
                    binding.textHome.text = "Raté"
                }
            }
            else
            {
                binding.textHome.text = "Mauvais bouton"
            }
        }
        else
        {
            binding.textHome.text = "Pas de note dans la queue"
        }

    }
    private fun initEvents()
    {
        // Find first beat
        nextBeat = beatsArray.getJSONObject(0).getDouble("start") * 1000
        // Find first segment
        while(true)
        {
            val segment = segmentsArray.getJSONObject(segIdx++)
            if(segment.getDouble("confidence")>=CONF_THRESH && segment.getDouble("start") * 1000 >= nextBeat + HALF_BEAT_MIN_MS)
            {
                // Save segment
                nextSeg = segment.getDouble("start") * 1000
                nextSegType = EventType.DRUM2
                break
            }
        }
    }
}
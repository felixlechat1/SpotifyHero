package com.example.test1.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.test1.R
import com.example.test1.databinding.FragmentHomeBinding
import com.example.test1.utils.ReadJSONFromAssets
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

const val OBJECT_SLIDE_PERIOD_S     = 500

class HomeFragment : Fragment() {
    private var beatIdx = 0
    private var nextBeat = 0.0
    private var currBeat = 0.0
    private lateinit var jsonArray : JSONArray
    private var start_time_ms = 0
    private var time_ms = 0
    private val timer = Timer()
    private var songPlaying = false
    private var _binding: FragmentHomeBinding? = null

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

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        binding.start.setOnClickListener {
            startSong()
        }
        binding.drum.setOnClickListener{
            drum()
        }

        // Read song data and initialize data structures
        val jsonString = ReadJSONFromAssets(this.context, "data.json")
        val jsonObj = JSONObject(jsonString)
        jsonArray = jsonObj.getJSONArray("beats")
        nextBeat = jsonArray.getJSONObject(0).getDouble("start") * 1000

        // Setup timer each 10ms
        timer.schedule(TimeTask(), 0, 5)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private inner class TimeTask: TimerTask()
    {
        override fun run()
        {
            if(songPlaying)
            {
                time_ms = System.currentTimeMillis().toInt() - start_time_ms
                // Time for next beat?
                if(time_ms + OBJECT_SLIDE_PERIOD_S >=nextBeat)
                {
                    // Launch animation
                    animate()
                    currBeat = nextBeat
                    nextBeat = jsonArray.getJSONObject(++beatIdx).getDouble("start") * 1000
                }
            }
        }
    }
    fun animate() {
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
//        binding.textHome.text = "Current timestamp: " + nextBeat.toString()
//        binding.textHome.text = "Delta : " + (time_ms - nextBeat).toString()
    }
    private fun startSong() {
        //TODO::Start playing song

        start_time_ms = System.currentTimeMillis().toInt()
        songPlaying = true
    }
    private fun drum() {
        //Click is timed correctly?
        if(time_ms<currBeat+200 && time_ms>currBeat+75)
        {
            binding.textHome.text = "OK"
        }
        else
        {
            binding.textHome.text = "PAS OK"
        }
    }
}
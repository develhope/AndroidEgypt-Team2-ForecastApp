package co.develhope.meteoapp.ui.tomorrow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import co.develhope.meteoapp.data.Data
import co.develhope.meteoapp.data.domain.DailyDataLocal
import co.develhope.meteoapp.data.domain.HourlyForecast
import co.develhope.meteoapp.databinding.FragmentTomorrowScreenBinding
import co.develhope.meteoapp.ui.search.adapter.DataSearches
import co.develhope.meteoapp.ui.today.adapter.HourlyForecastItems
import co.develhope.meteoapp.ui.tomorrow.adapter.TomorrowAdapter
import co.develhope.meteoapp.ui.tomorrow.viewModel.TomorrowViewModel
import co.develhope.meteoapp.ui.util.DataState
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter

@AndroidEntryPoint

class TomorrowScreenFragment : Fragment() {
    private var _binding: FragmentTomorrowScreenBinding? = null
    private val binding get() = _binding!!
    private val tomorrowViewModel: TomorrowViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTomorrowScreenBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataSearches = Data.getSearchCity(requireContext())

        var defaultData = DataSearches.ItemSearch(
            longitude = 12.51,
            latitude = 41.89,
            recentCitySearch = "Roma",
            admin1 = "Lazio"
        )

        var longitude = defaultData.longitude
        var latitude = defaultData.latitude

        if (dataSearches is DataSearches.ItemSearch) {
            longitude = dataSearches.longitude
            latitude = dataSearches.latitude
        }

        val selectedDate = Data.getSavedDate()!!.format(DateTimeFormatter.ofPattern("YYYY-MM-d"))
        Log.d("DATE", selectedDate!!)

        setupAdapter()
        collectData()
        tomorrowViewModel.getTomorrowWeather(latitude!!, longitude!!, selectedDate, selectedDate)
    }

    private fun collectData() {
        tomorrowViewModel.result.observe(viewLifecycleOwner) {
            when (it) {
                is DataState.Failure -> it.throwable.message
                is DataState.Loading -> binding.tomorrowProgress.visibility = View.VISIBLE
                is DataState.Success -> {
                    binding.tomorrowProgress.visibility = View.GONE
                    (binding.tomorrowRecyclerview.adapter as TomorrowAdapter).setNewList(it.data.toHourlyForecastItems())
                }
                else -> {
                    "try again"
                }
            }
        }

    }


    private fun setupAdapter() {
        binding.tomorrowRecyclerview.adapter = TomorrowAdapter(listOf())
    }

    private fun DailyDataLocal?.toHourlyForecastItems(): List<HourlyForecastItems> {
        val newList = mutableListOf<HourlyForecastItems>()

        newList.add(
            HourlyForecastItems.Title(
                Data.getCityLocation(requireContext()),
                OffsetDateTime.now()
            )
        )

        this?.forEach { hourly ->
            newList.add(
                HourlyForecastItems.Forecast(
                    HourlyForecast(
                        date = hourly.time,
                        hourlyTemp = hourly.temperature2m?.toInt() ?: 0,
                        possibleRain = hourly.rainChance ?: 0,
                        apparentTemp = hourly.apparentTemperature?.toInt() ?: 0,
                        uvIndex = hourly.uvIndex?.toInt() ?: 0,
                        humidity = hourly.humidity ?: 0,
                        windDirection = hourly.windDirection.toString(),
                        windSpeed = hourly.windSpeed?.toInt() ?: 0,
                        cloudyness = hourly.cloudCover ?: 0,
                        rain = hourly.rain?.toInt() ?: 0,
                        forecastIndex = hourly.weathercode ?: 0,
                        isDay = hourly.isDay ?: 0
                    )
                )
            )
        }
        return newList
    }
}
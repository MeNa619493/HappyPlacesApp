package com.example.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.happyplaces.R
import com.example.happyplaces.adapters.HappyPlaceAdapter
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import com.example.happyplaces.utils.SwipeToDeleteCallback
import com.example.happyplaces.utils.SwipeToEditCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fabAddHappyPlaces.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }

        getHappyPlacesListFromLocalDB()
    }

    private fun getHappyPlacesListFromLocalDB(){
        val dbHandler = DatabaseHandler(this)
        val getHappyPlacesList: ArrayList<HappyPlaceModel> = dbHandler.getHappyPlacesList()

        if (getHappyPlacesList.size > 0){
            rv_happy_places_list.visibility = View.VISIBLE
            tv_no_records_available.visibility = View.GONE
            setupHappyPlacesRecyclerView(getHappyPlacesList)
        }
        else{
            rv_happy_places_list.visibility = View.GONE
            tv_no_records_available.visibility = View.VISIBLE
        }
    }

    private fun setupHappyPlacesRecyclerView(happyPlaceList: ArrayList<HappyPlaceModel>){
        rv_happy_places_list.layoutManager = LinearLayoutManager(this)
        rv_happy_places_list.setHasFixedSize(true)
        val placesAdapter = HappyPlaceAdapter(this, happyPlaceList)
        rv_happy_places_list.adapter = placesAdapter

        placesAdapter.setOnClickListener(object: HappyPlaceAdapter.OnClickListener{
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(this@MainActivity, HappyPlaceDetailActivity::class.java)
                intent.putExtra(EXTRA_PLACE_DETAILS, model)
                startActivity(intent)
            }
        })

        val editSwipeHandler = object :SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_places_list.adapter as HappyPlaceAdapter
                adapter.notifyEditItem(this@MainActivity,viewHolder.bindingAdapterPosition,
                    ADD_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }

        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(rv_happy_places_list)

        val deleteSwipeHandler = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = rv_happy_places_list.adapter as HappyPlaceAdapter
                getHappyPlacesListFromLocalDB()
            }
        }

        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(rv_happy_places_list)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                getHappyPlacesListFromLocalDB()
            }
            else{
                Log.e("Activity", "Cancelled or back pressed")
            }
        }
    }
}
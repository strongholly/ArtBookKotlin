package com.example.artbookkotlin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.artbookkotlin.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var artList: ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.toolbar)

        // RecyclerView setup
        artList = ArrayList<Art>()
        binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        artAdapter = ArtAdapter(artList)
        binding.recyclerView.adapter = artAdapter

        // Fetching data from the database
        try {
            val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
            val cursor = database.rawQuery("SELECT * FROM arts", null)

            val artNameIx = cursor.getColumnIndex("artname")
            val idIX = cursor.getColumnIndex("id")

            while (cursor.moveToNext()) {
                val name = cursor.getString(artNameIx)
                val id = cursor.getInt(idIX)

                val art = Art(name, id)
                artList.add(art)
            }

            artAdapter.notifyDataSetChanged()
            cursor.close()
            database.close()  // Veritabanı bağlantısını kapatıyoruz

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Menuyu inflater ile bağlayacağız
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_art_item) {
            // Intent oluşturulmalı önce veri eklenmeli sonra activity başlatılmalı
            val intent = Intent(this@MainActivity, ArtActivity::class.java)
            intent.putExtra("info", "new")  // Veriyi gönderiyoruz
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}

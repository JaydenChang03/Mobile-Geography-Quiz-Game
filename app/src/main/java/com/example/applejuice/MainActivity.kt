package com.example.applejuice

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayout
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var itemRecyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var emptyStateView: TextView
    private var allItems: List<Item> = listOf()
    private var isListView = true
    private lateinit var viewToggleMenuItem: MenuItem
    private lateinit var tabLayout: TabLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {

            val toolbar: Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            itemRecyclerView = findViewById(R.id.item_recycler_view)
            itemRecyclerView.layoutManager = LinearLayoutManager(this)

            emptyStateView = findViewById(R.id.empty_state_view)

            tabLayout = findViewById(R.id.tabLayout)

            setupTabLayout()

            val fab: FloatingActionButton = findViewById(R.id.fab_add_item)
            fab.setOnClickListener {
                val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_animation)
                fab.startAnimation(scaleAnimation)
                val intent = Intent(this@MainActivity, AddItemActivity::class.java)
                startActivity(intent)
            }

            updateUI()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            try {
                ItemDatabase.get(applicationContext).itemDao().getItems().collect { items ->
                    allItems = items
                    adapter = ItemAdapter(items)
                    itemRecyclerView.adapter = adapter

                    updateEmptyState(items)
                    setupTabLayout()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in updateUI", e)
                Toast.makeText(this@MainActivity, "Failed to load items: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupTabLayout() {
        tabLayout.removeAllTabs()
        val categories = getCategoriesWithCounts()
        categories.forEach { (category, count) ->
            tabLayout.addTab(tabLayout.newTab().setText("$category ($count)"))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterItemsByTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun getCategoriesWithCounts(): List<Pair<String, Int>> {
        val categoryCounts = allItems.groupBy { it.category }
            .mapValues { it.value.size }
        val allCount = allItems.size
        return listOf(Pair("All", allCount)) + categoryCounts.toList()
    }

    private fun filterItemsByTab(tabPosition: Int) {
        val categories = getCategoriesWithCounts()
        val filteredItems = when (tabPosition) {
            0 -> allItems // "All" category
            else -> allItems.filter { it.category == categories[tabPosition].first }
        }
        adapter.updateItems(filteredItems)
        updateEmptyState(filteredItems)
    }

    private fun updateEmptyState(items: List<Item>) {
        if (items.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
            itemRecyclerView.visibility = View.GONE
        } else {
            emptyStateView.visibility = View.GONE
            itemRecyclerView.visibility = View.VISIBLE
        }
    }




    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        viewToggleMenuItem = menu.findItem(R.id.action_toggle_view)
        updateViewToggleIcon()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_toggle_view -> {
                toggleView()
                true
            }
            R.id.action_refresh -> {
                updateUI()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortDialog() {
        val options = listOf(
            Pair("Title") { it: Item -> it.title },
            Pair("Date") { it: Item -> it.date },
            Pair("Category") { it: Item -> it.category }
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sort by")

        val inflater = LayoutInflater.from(this)
        builder.setAdapter(
            object : ArrayAdapter<Pair<String, (Item) -> Comparable<*>>>(
                this,
                R.layout.sort_dialog_item,
                options
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: inflater.inflate(R.layout.sort_dialog_item, parent, false)
                    val icon = view.findViewById<ImageView>(R.id.icon)
                    val text = view.findViewById<TextView>(R.id.text)
                    val option = options[position]

                    icon.setImageResource(R.drawable.ic_sort)
                    text.text = option.first
                    return view
                }
            }
        ) { _, which ->
            val sortedItems = allItems.sortedWith(compareBy(options[which].second))
            adapter.updateItems(sortedItems)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun toggleView() {
        isListView = !isListView
        updateViewToggleIcon()
        updateRecyclerViewLayout()
    }

    private fun updateViewToggleIcon() {
        viewToggleMenuItem.setIcon(if (isListView) R.drawable.ic_grid else R.drawable.ic_list)
    }

    private fun updateRecyclerViewLayout() {
        itemRecyclerView.layoutManager = if (isListView) {
            LinearLayoutManager(this)
        } else {
            GridLayoutManager(this, 2)
        }
        adapter.notifyDataSetChanged()
    }


    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
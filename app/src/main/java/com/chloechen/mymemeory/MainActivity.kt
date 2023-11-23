package com.chloechen.mymemeory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chloechen.mymemeory.models.BoardSize
import com.chloechen.mymemeory.models.MemoryGame
import com.chloechen.mymemeory.utils.EXTRA_BOARD_SIZE
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248
    }

    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit  var tvNumMoves: TextView
    private lateinit var tvNumParis: TextView

    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private var boardSize: BoardSize = BoardSize.EASY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumParis = findViewById(R.id.tvNumPairs)

        val intent = Intent(this, CreateActivity:: class.java)
        intent.putExtra(EXTRA_BOARD_SIZE, BoardSize.MEDIUM)
        startActivity(intent)

//        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
//        val randomizedImages = (chosenImages + chosenImages).shuffled()
//        val memoryCards = randomizedImages.map { MemoryCard(it) }


        setupBoard()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh->{
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGane()){
                    showAlertDialog("Quit you current game?", null, View.OnClickListener {
                        setupBoard()
                    })
                }else{
                    setupBoard()
                }
                //set up the game again
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
//            R.id.mi_download -> {
//                showDownloadDialog()
//                return true
//            }
//            R.id.mi_about -> {
//                firebaseAnalytics.logEvent("open_about_link", null)
//                val aboutLink = remoteConfig.getString("about_link")
//                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aboutLink)))
//                return true
//            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra("EXTRA_GAME_NAME")
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
           // downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

//    private fun showDownloadDialog() {
//        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
//        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
//            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
//            val gameToDownload = etDownloadGame.text.toString().trim()
//            downloadGame(gameToDownload)
//        })
//    }


//    private fun downloadGame(customGameName: String) {
//        if (customGameName.isBlank()) {
//            Snackbar.make(clRoot, "Game name can't be blank", Snackbar.LENGTH_LONG).show()
//            Log.e(TAG, "Trying to retrieve an empty game name")
//            return
//        }
//        firebaseAnalytics.logEvent("download_game_attempt") {
//            param("game_name", customGameName)
//        }
//        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
//            val userImageList = document.toObject(UserImageList::class.java)
//            if (userImageList?.images == null)   {
//                Log.e(TAG, "Invalid custom game data from Firebase")
//                Snackbar.make(clRoot, "Sorry, we couldn't find any such game, '$customGameName'", Snackbar.LENGTH_LONG).show()
//                return@addOnSuccessListener
//            }
//            firebaseAnalytics.logEvent("download_game_success") {
//                param("game_name", customGameName)
//            }
//            val numCards = userImageList.images.size * 2
//            boardSize = BoardSize.getByValue(numCards)
//            customGameImages = userImageList.images
//            gameName = customGameName
//            // Pre-fetch the images for faster loading
//            for (imageUrl in userImageList.images) {
//                Picasso.get().load(imageUrl).fetch()
//            }
//            Snackbar.make(clRoot, "You're now playing '$customGameName'!", Snackbar.LENGTH_LONG).show()
//            setupBoard()
//        }.addOnFailureListener { exception ->
//            Log.e(TAG, "Exception when retrieving game", exception)
//        }
//    }

    private fun showCreationDialog() {

       // firebaseAnalytics.logEvent("creation_show_dialog", null)
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
//            firebaseAnalytics.logEvent("creation_start_activity") {
//                param("board_size", desiredBoardSize.name)
//            }
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }


    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            //set new value for board size
            boardSize = when(radioGroupSize.checkedRadioButtonId){
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }

            setupBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveButtonClickListener: View.OnClickListener ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK"){ _, _ ->
                positiveButtonClickListener.onClick(null)

            }.show()
    }

    private fun setupBoard(){
        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumParis.text = "Paris: 0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Easy: 6 x 3"
                tvNumParis.text = "Paris: 0/9"

            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Easy: 6 x 6"
                tvNumParis.text = "Paris: 0/12"
            }
        }
        tvNumParis.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize)
        adapter = MemoryBoardAdapter(this, boardSize,memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClick(position: Int) {
                updateGameWithFlip(position)
                //Log.i(TAG, "Card clicked $position")
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }


    private fun updateGameWithFlip(position: Int) {
        if(memoryGame.haveWonGane()){
            Snackbar.make(clRoot, "You already won! Use the menu to play again.", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot, "invalid move!", Snackbar.LENGTH_SHORT).show()
            return
        }
        // Actually flip over the card
        if(memoryGame.flipCard(position)){
            Log.i(TAG, "Found a Match! Num pairs found: ${memoryGame.numPairsFound}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full),
            ) as Int
            tvNumParis.setTextColor(color)

            tvNumParis.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGane()){
                Snackbar.make(clRoot, "You won! Congratulation.", Snackbar.LENGTH_LONG).show()
            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}
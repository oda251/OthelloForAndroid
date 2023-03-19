package com.conpeitomorimori.othello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conpeitomorimori.othello.ui.theme.OthelloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OthelloTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    OthelloApp()
                }
            }
        }
    }
}

enum class Piece {
    BLACK{
        override fun toString(): String {
            return "●"
        }
    },
    WHITE{
        override fun toString(): String {
            return "〇"
        }
    },
    NONE{
        override fun toString(): String {
            return ""
        }
    };

    fun toImage(): Int {
        return when(this) {
            BLACK -> R.drawable.black
            WHITE -> R.drawable.white
            else -> R.drawable.none
        }
    }
}

@Composable
fun OthelloApp() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(35, 35, 34)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            Board()
        }
    }
}

@Composable
fun Board() {
    var currentTurn by remember { mutableStateOf(Piece.BLACK) }
    fun switchTurn() {
        currentTurn = when (currentTurn) {
            Piece.BLACK -> Piece.WHITE
            else -> Piece.BLACK
        }
    }

    fun initializer():Array<Array<Piece>> {
        val pieceArr = Array(8) {
            Array(8) { Piece.NONE }
        }
        pieceArr[3][3] = Piece.WHITE
        pieceArr[4][4] = Piece.WHITE
        pieceArr[3][4] = Piece.BLACK
        pieceArr[4][3] = Piece.BLACK

        return pieceArr
    }

    var pieceArr by remember{ mutableStateOf(initializer()) }

    val boardLog = remember { mutableStateListOf<Array<Array<Piece>>>() }

    fun doOver() {
        if (boardLog.size > 0) {
            pieceArr = boardLog.last()
            boardLog.removeLast()
            switchTurn()
        }
    }

    // コマ数をカウント
    var amountOfBlack by remember { mutableStateOf(2) }
    var amountOfWhite by remember { mutableStateOf(2) }

    fun countPiece() {
        var tempB = 0
        var tempW = 0
        for (arr in pieceArr) {
            tempB += arr.count { it == Piece.BLACK }
            tempW += arr.count { it == Piece.WHITE }
        }
        amountOfBlack = tempB
        amountOfWhite = tempW
    }

    // 配置時チェックのための、縦横斜めの配列を取得
    fun getSquareLineSet(pieceID:Pair<Int, Int>):List<List<Pair<Int, Int>>> {
        // 縦
        val vertical = mutableListOf<Pair<Int, Int>>()
        repeat(8) { vertical.add(Pair(pieceID.first, it)) }
        // 横
        val horizontal = mutableListOf<Pair<Int, Int>>()
        repeat(8) { horizontal.add(Pair(it, pieceID.second)) }
        // 右肩上がり
        val up = mutableListOf<Pair<Int, Int>>()
        var tempPair = arrayOf(0, pieceID.first + pieceID.second)
        repeat(8) {
            if (tempPair.first() in 0..7 && tempPair.last() in 0..7) {
                up.add(Pair(tempPair.first(), tempPair.last()))
            }
            tempPair[0] += 1
            tempPair[1] -= 1
        }
        // 右肩下がり
        val down = mutableListOf<Pair<Int, Int>>()
        tempPair = arrayOf(0, pieceID.second - pieceID.first)
        repeat(8) {
            if (tempPair.first() in 0..7 && tempPair.last() in 0..7) {
                down.add(Pair(tempPair.first(), tempPair.last()))
            }
            repeat(2) {
                tempPair[it] += 1
            }
        }
        // convert to immutable
        vertical.toList()
        horizontal.toList()
        up.toList()
        down.toList()

        return listOf(vertical, horizontal, up, down)
    }

    fun puttingCheck(pieceID: Pair<Int, Int>) {
        var updated = false
        val squareLineSetToCheck = getSquareLineSet(pieceID)
        for (squareLine in squareLineSetToCheck) {
            // 検証中のリスト内で、置きたい位置のインデックスを調べる
            val indexOfPieceToPut = squareLine.indexOf(pieceID)
            // 検証のリストは位置座標のみ保持しているため、対応する色のリストを取得
            val colorOfSquareLine = squareLine.map { pieceArr[it.first][it.second] }
            // 現在の色と反対の色を調べる
            val oppositeColor = when(currentTurn) { Piece.BLACK -> Piece.WHITE else -> Piece.BLACK }
            // 負方向で検証
            if (indexOfPieceToPut >= 2) {
                // indexOf, lastIndexOfに第二引数を渡すと"too much argument"エラーが発生するため、このようにした
                val indexOfNearestSameColorPiece = colorOfSquareLine.subList(0, indexOfPieceToPut - 1).lastIndexOf(currentTurn)
                if (indexOfNearestSameColorPiece != -1) {
                    // 間のコマが全て反対の色かどうか調べる
                    if (colorOfSquareLine.subList(indexOfNearestSameColorPiece + 1, indexOfPieceToPut).all { it == oppositeColor }) {
                        if (!updated) {
                            val newArr = Array(8) { index ->
                                pieceArr[index].copyOf()
                            }
                            boardLog.add(newArr)
                            updated = true
                        }
                        for (elem in squareLine.subList(indexOfNearestSameColorPiece + 1, indexOfPieceToPut)) {
                            pieceArr[elem.first][elem.second] = currentTurn
                        }
                    }
                }
            }
            // 正方向で検証
            if (squareLine.size - indexOfPieceToPut > 2) {
                var indexOfNearestSameColorPiece = colorOfSquareLine.subList(indexOfPieceToPut + 2, squareLine.size).indexOf(currentTurn)
                if (indexOfNearestSameColorPiece != -1) {
                    // 同色のコマを探す際の、探索開始位置の分のズレを調整
                    indexOfNearestSameColorPiece +=  indexOfPieceToPut + 2
                    if (colorOfSquareLine.subList(indexOfPieceToPut + 1, indexOfNearestSameColorPiece).all { it == oppositeColor }) {
                        if (!updated) {
                            val newArr = Array(8) { index ->
                                pieceArr[index].copyOf()
                            }
                            boardLog.add(newArr)
                            updated = true
                        }
                        for (elem in squareLine.subList(indexOfPieceToPut + 1, indexOfNearestSameColorPiece)) {
                            pieceArr[elem.first][elem.second] = currentTurn
                        }
                    }
                }
            }
        }
        if (updated) {
            pieceArr[pieceID.first][pieceID.second] = currentTurn
            switchTurn()
            countPiece()
        }
    }

    fun handleClick(pieceID: Pair<Int, Int>) {
        if (pieceArr[pieceID.first][pieceID.second] == Piece.NONE) {
            puttingCheck(pieceID)
        }
    }

    Column(
        modifier = Modifier
    ) {
        pieceArr.forEachIndexed { rowID, pieceLine ->
            SquareLine(arr = pieceLine, rowID = rowID, onClick = { handleClick(it) })
        }
        Spacer(modifier = Modifier.height(5.dp))
        Column (
            modifier = Modifier
                .background(color = Color(130, 110, 110), shape = RoundedCornerShape(15))
                .padding(vertical = 6.dp, horizontal = 15.dp)
        ) {
            Text(
                text = when (currentTurn) {
                    Piece.BLACK -> stringResource(R.string.blacksTurn)
                    else -> stringResource(
                        R.string.whitesTurn
                    )
                },
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.showAmount, amountOfBlack, amountOfWhite),
                fontSize = 15.sp,
                color = Color.White
            )
        }
        Button(onClick = {
            pieceArr = initializer()
            currentTurn = Piece.BLACK
            boardLog.clear()
            countPiece()
        }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(160,40,30))) {
            Text(text = stringResource(R.string.newGame), color = Color.White, fontSize = 20.sp)
        }
        Button(onClick = { switchTurn() }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)) {
            Text(text = stringResource(R.string.skip), color = Color.White, fontSize = 20.sp)
        }
        Button(
            onClick = {
                doOver()
                countPiece()
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.DarkGray)
        ) {
            Text(text = stringResource(R.string.do_over), color = Color.White, fontSize = 20.sp)
        }
    }
}

@Composable
fun SquareLine(arr: Array<Piece>, onClick: (Pair<Int, Int>) -> Unit, rowID: Int) {
    Row {
        arr.forEachIndexed { columnID, pieceStatus ->
            Square(
                piece = pieceStatus,
                modifier = Modifier.size(40.dp),
                pieceID = Pair(rowID, columnID),
                onClick = { onClick(it) }
            )
        }
    }
}

@Composable
fun Square(piece: Piece, modifier: Modifier = Modifier, pieceID: Pair<Int, Int>, onClick: (Pair<Int, Int>) -> Unit) {
    Box(
        modifier = modifier
            .border(width = 1.dp, color = Color.LightGray, shape = RectangleShape)
            .padding(all = 0.dp)
            .clickable { onClick(pieceID) }
            .background(color = Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (pieceID == Pair(7,7)) {
            Image(
                painter = painterResource(id = R.drawable.icon),
                contentDescription = null,
                alpha = 0.5F
            )
        }
        if (piece != Piece.NONE) {
            Image(
                painter = painterResource(piece.toImage()),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    OthelloTheme {
        OthelloApp()
    }
}
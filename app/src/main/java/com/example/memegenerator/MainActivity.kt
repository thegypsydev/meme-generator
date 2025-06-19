package com.example.memegenerator

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemeGeneratorApp()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemeGeneratorApp() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val defaultText = stringResource(id = R.string.default_placeholder_text)
    var text by remember { mutableStateOf("") }
    val originalBitmap = remember { mutableStateOf<Bitmap?>(null) }

    var fontSize by remember { mutableStateOf(24f) }
    var textColor by remember { mutableStateOf(Color.Black) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var textOffset by remember { mutableStateOf(Offset(100f, 100f)) }
    var textSize by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            coroutineScope.launch {
                originalBitmap.value = loadBitmapFromUri(context, uri)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, Color.Gray)

        ) {

            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.deafult_meme),
                    contentDescription = "Placeholder Image",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            BoxWithConstraints {
                val imageWidth = constraints.maxWidth.toFloat()
                val imageHeight = constraints.maxHeight.toFloat()


                Text(
                    text = text,
                    style = TextStyle(
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = FontFamily.Serif,
                        fontSize = fontSize.sp,
                        color = textColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = TextDecoration.combine(
                            listOfNotNull(
                                if (isUnderline) TextDecoration.Underline else null
                            )
                        )
                    ),
                    modifier = Modifier
                        .offset { IntOffset(textOffset.x.roundToInt(), textOffset.y.roundToInt()) }
                        .onGloballyPositioned { layoutCoordinates ->
                            textSize = layoutCoordinates.size
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                val newX = textOffset.x + dragAmount.x
                                val newY = textOffset.y + dragAmount.y

                                val maxX = (imageWidth - textSize.width).coerceAtLeast(0f)
                                val maxY = (imageHeight - textSize.height).coerceAtLeast(0f)

                                val clampedX = newX.coerceIn(0f, maxX)
                                val clampedY = newY.coerceIn(0f, maxY)
                                textOffset = Offset(clampedX, clampedY)

                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints {
            val imageWidth = constraints.maxWidth.toFloat()
            val imageHeight = constraints.maxHeight.toFloat()
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it

                    text = it

                    val maxX = (imageWidth - textSize.width).coerceAtLeast(0f)
                    val maxY = (imageHeight - textSize.height).coerceAtLeast(0f)

                    val clampedX = textOffset.x.coerceIn(0f, maxX)
                    val clampedY = textOffset.y.coerceIn(0f, maxY)

                    textOffset = Offset(clampedX, clampedY)
                },
                modifier = Modifier
                    .background(Color.LightGray)
                    .padding(8.dp)
                    .fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = defaultText,
                                style = TextStyle(color = Color.Gray)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Button(
                onClick = { pickImageLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Select Image")
            }
            Button(
                onClick = { imageUri = null },
                modifier = Modifier.weight(1f)
            ) {
                Text("Remove Image")
            }
            Button(
                onClick = {
                    text = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Text")
            }

            Button(onClick = {
                coroutineScope.launch {
                    val bitmap = originalBitmap.value
                    if (bitmap != null) {
                        val capturedBitmap = captureMemeBitmapSuspend(
                            context = context,
                            bitmap = bitmap,
                            text = text,
                            fontSize = fontSize,
                            textColor = textColor,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            textOffset = textOffset
                        )
                        saveMemeToGallery(context, capturedBitmap)
                    }
                }
            }) {
                Text("Save Meme")
            }


            Button(onClick = {
                coroutineScope.launch {
                    val bitmap = originalBitmap.value

                    if (bitmap != null) {
                        val capturedBitmap = captureMemeBitmapSuspend(
                            context = context,
                            bitmap = bitmap,
                            text = text,
                            fontSize = fontSize,
                            textColor = textColor,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            textOffset = textOffset
                        )
                        shareMeme(context, capturedBitmap)
                    } else {
                        Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Share Meme")
            }

        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Font Size: ${fontSize.toInt()}")
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 10f..60f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val predefinedColors = listOf(Color.White, Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow)
            predefinedColors.forEach { color ->
                Button(
                    onClick = { textColor = color },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {}
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconToggleButton(checked = isBold, onCheckedChange = { isBold = it }) {
                Text("B", fontWeight = FontWeight.Bold)
            }
            IconToggleButton(checked = isItalic, onCheckedChange = { isItalic = it }) {
                Text("I", style = TextStyle(fontStyle = FontStyle.Italic))
            }
            IconToggleButton(checked = isUnderline, onCheckedChange = { isUnderline = it }) {
                Text("U", textDecoration = TextDecoration.Underline)
            }
        }
    }
}


suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        val loader = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .build()
        val result = (ImageLoader(context).execute(loader) as? SuccessResult)
        val drawable = result?.drawable
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = drawable?.intrinsicWidth ?: 1
            val height = drawable?.intrinsicHeight ?: 1
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable?.setBounds(0, 0, canvas.width, canvas.height)
            drawable?.draw(canvas)
            bmp
        }
        bitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }
}


@Composable
fun MemeContent(
    bitmap: Bitmap?,
    text: String,
    fontSize: Float,
    textColor: Color,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    textOffset: Offset,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize.sp,
                color = textColor,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = TextDecoration.combine(
                    listOfNotNull(if (isUnderline) TextDecoration.Underline else null)
                )
            ),
            modifier = Modifier.offset {
                IntOffset(textOffset.x.roundToInt(), textOffset.y.roundToInt())
            }
        )
    }
}


suspend fun captureMemeBitmapSuspend(
    context: Context,
    bitmap: Bitmap,
    text: String,
    fontSize: Float,
    textColor: Color,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    textOffset: Offset
): Bitmap = withContext(Dispatchers.Main) {
    val latch = CountDownLatch(1)
    var capturedBitmap: Bitmap? = null

    val width = bitmap.width
    val height = bitmap.height

    val composeView = ComposeView(context).apply {
        layoutParams = ViewGroup.LayoutParams(width, height)
        visibility = View.INVISIBLE
        setContent {
            MemeContent(
                bitmap = bitmap,
                text = text,
                fontSize = fontSize,
                textColor = textColor,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                textOffset = textOffset,
                modifier = Modifier.size(width.dp, height.dp)
            )
        }
    }

    val decorView = (context as? ComponentActivity)?.window?.decorView as? ViewGroup
    decorView?.addView(composeView)

    Handler(Looper.getMainLooper()).postDelayed({
        capturedBitmap = composeView.drawToBitmap(Bitmap.Config.ARGB_8888)
        decorView?.removeView(composeView)
        latch.countDown()
    }, 300)

    withContext(Dispatchers.IO) {
        latch.await()
    }

    return@withContext capturedBitmap!!
}

fun saveMemeToGallery(context: Context, bitmap: Bitmap) {
    val filename = "meme_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Memes")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it).use { stream ->
            stream?.let { it1 -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, it1) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
    } ?: Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
}


fun shareMeme(context: Context, bitmap: Bitmap) {
    val file = File(context.cacheDir, "shared_meme_${UUID.randomUUID()}.png")
    FileOutputStream(file).use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Meme"))
}

@Preview(showBackground = true)
@Composable
fun MemeGeneratorAppPreview() {
    MemeGeneratorApp()
}

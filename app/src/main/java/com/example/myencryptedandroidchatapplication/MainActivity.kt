package com.example.myencryptedandroidchatapplication

import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myencryptedandroidchatapplication.ui.theme.MyEncryptedAndroidChatApplicationTheme
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// 📦 Message Model (IMPORTANT: default values needed for Firebase)
data class Message(
    val text: String = "",
    val time: String = "",
    val date: String = ""
)

class MainActivity : ComponentActivity() {

    private val dbRef = FirebaseDatabase.getInstance().getReference("messages")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyEncryptedAndroidChatApplicationTheme {
                ChatScreen(dbRef)
            }
        }
    }
}

@Composable
fun ChatScreen(dbRef: DatabaseReference) {

    var message by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }

    val key = "1234567890123456" // demo AES key

    // 🔄 REAL-TIME LISTENER
    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<Message>()
                for (child in snapshot.children) {
                    val msg = child.getValue(Message::class.java)
                    msg?.let { tempList.add(it) }
                }
                messages = tempList
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = "🔐 Encrypted Chat App",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 📩 Chat Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->

                val decrypted = decrypt(msg.text, key)

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = decrypted)

                    Text(
                        text = "${msg.time} • ${msg.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ✍️ Input + Send
        Row(modifier = Modifier.fillMaxWidth()) {

            TextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter message") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (message.isNotEmpty()) {

                    val (time, date) = getCurrentTimeDate()
                    val encrypted = encrypt(message, key)

                    val msgObj = Message(encrypted, time, date)

                    dbRef.child(System.currentTimeMillis().toString())
                        .setValue(msgObj)

                    message = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}

/* ⏰ Time + Date */
fun getCurrentTimeDate(): Pair<String, String> {
    val date = Date()

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    return Pair(
        timeFormat.format(date),
        dateFormat.format(date)
    )
}

/* 🔐 AES Encryption */
fun encrypt(message: String, key: String): String {
    val cipher = Cipher.getInstance("AES")
    val secretKey = SecretKeySpec(key.toByteArray(), "AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encrypted = cipher.doFinal(message.toByteArray())
    return Base64.encodeToString(encrypted, Base64.DEFAULT)
}

fun decrypt(encryptedMessage: String, key: String): String {
    val cipher = Cipher.getInstance("AES")
    val secretKey = SecretKeySpec(key.toByteArray(), "AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    val decoded = Base64.decode(encryptedMessage, Base64.DEFAULT)
    return String(cipher.doFinal(decoded))
}
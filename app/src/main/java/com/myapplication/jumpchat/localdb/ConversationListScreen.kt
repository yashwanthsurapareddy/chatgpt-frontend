package com.myapplication.jumpchat.localdb

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    navController: NavController? = null,
    viewModel: ConversationListViewModel = viewModel(),
    onConversationClick: ((Long) -> Unit)? = null
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredConversations by viewModel.filteredConversations.collectAsState()
    val insets = WindowInsets.statusBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(insets)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .shadow(0.5.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                placeholder = {
                    Text(
                        text = "Search",
                        color = Color.DarkGray,
                        fontSize = 13.sp
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Gray,
                    unfocusedIndicatorColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    viewModel.createNewConversation { convoId ->
                        onConversationClick?.invoke(convoId)
                        navController?.navigate("chat/$convoId")
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Default.Create, contentDescription = "New Chat", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredConversations, key = { it.id }) { convo ->
                ConversationListCard(convo = convo) {
                    onConversationClick?.invoke(convo.id)
                    navController?.navigate("chat/${convo.id}")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


@Composable
fun ConversationListCard(convo: Conversation, onClick: () -> Unit) {
    val viewModel: ConversationListViewModel = viewModel()
    var isEditing by remember { mutableStateOf(false) }
    var editedTitleState by remember {
        mutableStateOf(
            TextFieldValue(
                text = convo.title,
                selection = TextRange(convo.title.length)
            )
        )
    }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF2F2F2),
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 59.dp)
            .clickable(enabled = !isEditing) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedTitleState,
                        onValueChange = { editedTitleState = it },
                        singleLine = true,
                        maxLines = 1,
                        textStyle = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF2F2F2),
                            unfocusedContainerColor = Color(0xFFF2F2F2),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                isEditing = false
                                focusManager.clearFocus()
                                val newTitle = editedTitleState.text.trim()
                                if (newTitle.isNotBlank() && newTitle != convo.title) {
                                    viewModel.renameConversation(convo.copy(title = newTitle))
                                }
                            }
                        )
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(
                        text = if (convo.title.length > 30) convo.title.take(30) + "..." else convo.title,
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Last updated: ${
                        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                            .format(Date(convo.lastUpdated))
                    }",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = {
                    isEditing = true
                    editedTitleState = TextFieldValue(
                        text = convo.title,
                        selection = TextRange(convo.title.length)
                    )
                }) {
                    Icon(Icons.Default.EditNote, contentDescription = "Edit")
                }
                IconButton(onClick = {
                    viewModel.deleteConversation(convo)
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

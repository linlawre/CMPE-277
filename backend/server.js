const express = require("express");
const mongoose = require("mongoose");
const bcrypt = require("bcryptjs");
const cors = require("cors");
const dotenv = require("dotenv");
dotenv.config();

const User = require("./models/User");

const app = express();
app.use(cors());


// MUST HAVE THIS
app.use(express.json());


// --- Connect to MongoDB ---
mongoose.connect(process.env.MONGO_URI)
    .then(() => console.log("MongoDB Connected"))
    .catch(err => console.log(err));

// --- SIGNUP ---
app.post("/signup", async (req, res) => {
    console.log("Signup body:", req.body);  // debug

    const { email, password } = req.body;
    // Basic validation
    if (!email.includes("@"))
        return res.json({ success: false, message: "Invalid email" });

    if (password.length < 6)
        return res.json({ success: false, message: "Password too short" });

    // Check if user exists
    const exist = await User.findOne({ email });
    if (exist) return res.json({ success: false, message: "Email already exists" });

    // Hash password
    const hashed = await bcrypt.hash(password, 10);

    // Save new user
    await User.create({ email, password: hashed });

    return res.json({ success: true, message: "Signup successful" });
});

// --- LOGIN ---
app.post("/login", async (req, res) => {
    const { email, password } = req.body;
    console.log("login body:", req.body);  // debug

    const user = await User.findOne({ email });
    if (!user)
        return res.json({ success: false, message: "Incorrect email or password" });

    const match = await bcrypt.compare(password, user.password);
    if (!match)
        return res.json({ success: false, message: "Incorrect email or password" });

    return res.json({ success: true, message: "Login successful" });
});


/**
Tim's notes routes. To be edited later as they are placeholder w/ Guest
*/
const NoteSchema = new mongoose.Schema({
    date: { type: String, required: true },
    title: { type: String, required: true },
    user: { type: String, default: "guest" },
    description: { type: String, required: true }
});

const Note = mongoose.model("Note", NoteSchema);

// Debugging to test in Postman
app.get("/debug/users", async (req, res) => {
  try {
    const users = await User.find();
    console.log("🔍 USERS:", users);
    res.json(users);
  } catch (err) {
    console.error("Error reading users:", err);
    res.status(500).json({ error: err.message });
  }
});

// Debugging to test in Postman
app.get("/debug/notes", async (req, res) => {
  try {
    const notes = await Note.find();
    console.log("🔍 NOTES:", notes);
    res.json(notes);
  } catch (err) {
    console.error("Error reading notes:", err);
    res.status(500).json({ error: err.message });
  }
});



app.get("/notes", async (req, res) => {
  try {
    const notes = await Note.find();
    res.json(notes);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});


app.post("/notes", async (req, res) => {
  try {
    const { date, title, user, description } = req.body;

    if (!user || !date || !description || !title)
      return res
        .status(400)
        .json({ success: false, message: "Missing required fields" });


    const note = await Note.create({
      date,
      title,
      description,
      user,
    });

    return res.status(201).json({ success: true, note });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});


app.put("/notes/:id", async (req, res) => {
  try {
    const noteId = req.params.id;
    const { title, description, date } = req.body;

    if (!title || !description || !date) {
      return res.status(400).json({ success: false, message: "Missing required fields" });
    }

    const updatedNote = await Note.findByIdAndUpdate(
      noteId,
      { title, description, date },
      { new: true }
    );

    if (!updatedNote) {
      return res.status(404).json({ success: false, message: "Note not found" });
    }

    res.json({ success: true, note: updatedNote });
  } catch (err) {
    console.error("Error updating note:", err);
    res.status(500).json({ success: false, message: err.message });
  }
});

app.delete("/notes/:id", async (req, res) => {
    try {
        const { id } = req.params;

        const note = await Note.findByIdAndDelete(id);
        if (!note) return res.status(404).json({ success: false, message: "Note not found" });

        res.json({ success: true, message: "Note deleted" });
    } catch (err) {
        res.status(500).json({ success: false, message: err.message });
    }
});


/**
Tasks Routes
**/
const taskSchema = new mongoose.Schema({
  description: { type: String, required: true },
  location: { type: String, default: null },
  date: { type: String, required: true },
  done: { type: Boolean, default: false },
});

const Task = mongoose.model('Task', taskSchema);

app.get('/tasks', async (req, res) => {
  const tasks = await Task.find();
  res.json(tasks);
});


app.post('/tasks', async (req, res) => {
  const { description, location, date, done } = req.body;
  if (!description || !date) return res.status(400).json({ success: false, message: "Missing required fields" });
  const task = new Task({ description, location, date, done });
  await task.save();
  res.json(task);
});


app.put('/tasks/:id', async (req, res) => {
  const { id } = req.params;
  const { description, location, done } = req.body;
  const task = await Task.findByIdAndUpdate(id, { description, location, done }, { new: true });
  if (!task) return res.status(404).json({ success: false, message: "Task not found" });
  res.json(task);
});


app.delete('/tasks/:id', async (req, res) => {
  const { id } = req.params;
  const task = await Task.findByIdAndDelete(id);
  if (!task) return res.status(404).json({ success: false, message: "Task not found" });
  res.json({ success: true });
});



// --- Start Server ---
app.listen(process.env.PORT, () =>
    console.log("Server running on port " + process.env.PORT)
);


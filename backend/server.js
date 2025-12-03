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
Tim's notes routes.
*/

/**
Schema for Notes, could be moved if needed
*/
const NoteSchema = new mongoose.Schema({
    date: { type: String, required: true },
    title: { type: String, required: true },
    user: { type: String, default: "guest" },
    description: { type: String, required: true }
});

//Variable we use for Note DB
const Note = mongoose.model("Note", NoteSchema);

// Debugging to test in Postman to ensure I have proper access
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

// Debugging to test in Postman to ensure I have proper access
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


/** Return all notes
*/
app.get("/notes", async (req, res) => {
  try {
    const notes = await Note.find();
    res.json(notes);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
});


// Check if body contains all necessary info then create a note
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

//Edit a note with new info
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

//Delete a note with a given Id
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

//Task Schema
const taskSchema = new mongoose.Schema({
  description: { type: String, required: true },
  location: { type: String, default: null },
  date: { type: String, required: true },
  done: { type: Boolean, default: false },
  user: { type:String,required:true}
});


//Variable for Task DB
const Task = mongoose.model('Task', taskSchema);

//Retrieve all tasks
app.get('/tasks', async (req, res) => {
  const tasks = await Task.find();
  res.json(tasks);
});

//Create a task
app.post('/tasks', async (req, res) => {
  const { description, location, date, done,user } = req.body;
  if (!description || !date) return res.status(400).json({ success: false, message: "Missing required fields" });
  const task = new Task({ description, location, date, done,user });
  await task.save();
  res.json(task);
});

//Edit a task with new informations
app.put('/tasks/:id', async (req, res) => {
  const { id } = req.params;
  const { description, location, date, done, user } = req.body;
  const task = await Task.findByIdAndUpdate(
    id,
    { description, location, date, done, user },
    { new: true }
  );
  if (!task) return res.status(404).json({ success: false, message: "Task not found" });
  res.json(task);
});

//Delete a task w/ given ID
app.delete('/tasks/:id', async (req, res) => {
  const { id } = req.params;
  const task = await Task.findByIdAndDelete(id);
  if (!task) return res.status(404).json({ success: false, message: "Task not found" });
  res.json({ success: true });
});

//Show all tasks; used for Postman to check that I have access
app.get('/debug/tasks', async (req, res) => {
  try {
    const tasks = await Task.find();
    res.json({
      success: true,
      count: tasks.length,
      tasks: tasks
    });
  } catch (err) {
    console.error("Error fetching tasks:", err);
    res.status(500).json({ success: false, message: "Could not fetch" });
  }
});

//Change password, utilized with code from sign-up to ensure password is changed properly
app.post("/change-password", async (req, res) => {
    try {
        const { email, oldPassword, newPassword } = req.body;

        if (!email || !oldPassword || !newPassword) {
            return res.status(400).json({ success: false, message: "Missing required fields" });
        }

        const user = await User.findOne({ email });
        if (!user) {
            return res.status(404).json({ success: false, message: "User not found" });
        }
        const match = await bcrypt.compare(oldPassword, user.password);
        if (!match) {
            return res.status(401).json({ success: false, message: "Wrong password" });
        }
        const hashedNew = await bcrypt.hash(newPassword, 10);
        user.password = hashedNew;
        await user.save();
        res.json({ success: true, message: "Password updated successfully" });
    } catch (err) {
        console.error("Error changing password:", err);
        res.status(500).json({ success: false, message: "Couldn't change your password" });
    }
});


// --- Start Server ---
app.listen(process.env.PORT, () =>
    console.log("Server running on port " + process.env.PORT)
);


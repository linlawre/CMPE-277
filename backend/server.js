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

// --- Start Server ---
app.listen(process.env.PORT, () =>
    console.log("Server running on port " + process.env.PORT)
);

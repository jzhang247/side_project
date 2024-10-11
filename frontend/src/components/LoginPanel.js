import { useState } from "react"
import { Box, Button, Stack, TextField } from "@mui/material";

export default function LoginPanel() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");

    return <div>
        <Stack spacing={2} direction="column" maxWidth={0.25}>
            <TextField
                // required
                // id="standard-required"
                label="username"
                variant="standard"
                onChange={(event) => { setUsername(event.target.value); }}
            />
            <TextField
                id="standard-password-input"
                label="Password"
                type="password"
                autoComplete="current-password"
                variant="standard"
                onChange={(event) => { setPassword(event.target.value); }}
            />
            <Button variant="outlined" onClick={() => { console.log({ username: username, password: password }) }}>Login</Button>
            <Button variant="outlined">Signup</Button>
        </Stack>
    </div>;
}
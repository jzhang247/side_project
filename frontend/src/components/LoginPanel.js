import { useState } from "react"
import { Constants } from "./Constants";
// import { Box, Button, Stack, TextField } from "@mui/material";
import { getSession, setSession, setToken } from "./TokenManager";

export default function LoginPanel() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [serverResponse, setServerResponse] = useState("");


    return <div>
        {/* <Stack spacing={2} direction="column" maxWidth={0.25}>
            <TextField
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
            <Button variant="outlined" onClick={() => {
                console.log({ username: username, password: password })
            }}>Login</Button>
            <Button variant="outlined">Signup</Button>
        </Stack> */}
        <div>
            <div><input onChange={(event) => { setUsername(event.target.value); }}></input></div>
            <div><input onChange={(event) => { setPassword(event.target.value); }}></input></div>
            <button onClick={() => {
                fetch(`${Constants.BACKEND}/signup`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: username, password: password })
                }).then(response => {
                    response.text().then(text => {
                        setServerResponse(text);
                    });
                });

            }}>Signup</button>
            <button onClick={() => {
                fetch(`${Constants.BACKEND}/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: username, password: password })
                }).then(response => {
                    response.text().then(text => {
                        setSession(text);
                        setServerResponse(text);
                    });
                });
            }}>Login</button>
            <button onClick={() => {
                console.log(JSON.stringify({ username: username, sessionId: getSession() }));
                fetch(`${Constants.BACKEND}/get_token`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: username, sessionId: getSession() })
                }).then(response => {
                    response.text().then(text => {
                        setServerResponse(text);
                        setToken(text);
                    });
                });
            }}>GetToken</button>
            <div>{serverResponse}</div>
        </div>

    </div>;
}
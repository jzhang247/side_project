import { useState } from "react";
import { Constants } from "./Constants";






export function NewMerchandisePanel() {
    const [categoryList, setCategoryList] = useState(null);

    if (categoryList == null) {


        fetch(`${Constants.BACKEND}/list_merchandise_categories`, {
            method: 'GET',
            mode: 'no-cors',
            headers: {
                'Content-Type': 'application/json'
            },
        }).then(response => {
            console.log('Response Status:', response.status);
            console.log('Response Headers:', response.headers);
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json(); // or response.text() for plain text
        }).then(data => {
            console.log(data);
        }).catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });

    }

    return <div>
        123
    </div>;
}
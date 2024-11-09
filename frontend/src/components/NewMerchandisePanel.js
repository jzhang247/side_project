import { useEffect, useState } from "react";
import { Constants } from "./Constants";





export function NewMerchandisePanel() {
    const [categories, setCategories] = useState("uninitialized");
    const [selectedCategoryName, setSelectedCategory] = useState(null);
    const [showCategories, setShowCategories] = useState(true);
    const [attributes, setAttributes] = useState(new Map());
    const [files, setFiles] = useState([]);
    const [message, setMessage] = useState("");
    const [showUpload, setShowUpload] = useState(false);
    const [showSearch, setShowSearch] = useState(false);
    const [filters, setFilters] = useState(new Map());
    const [searchResult, setSearchResult] = useState([]);


    useEffect(() => {
        console.log("fetching");

        fetch(`${Constants.BACKEND}/list_merchandise_categories`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
        }).then(response => {
            if (!response.ok) {
                console.log(response);
            } else {
                setCategories("failed");
            }
            response.json().then(category_js => {
                let list = category_js.categories;
                let categories = new Map();
                console.log(list);
                list.forEach(cate => {
                    categories.set(cate.name, cate);
                });
                setCategories(categories);


            });
        });
    }, []);



    if (typeof (categories) === typeof ('')) {
        return <div>{categories}</div>;
    }

    let categoriesList = [];
    categories.forEach((v, k) => {
        categoriesList.push(k);
    });
    categoriesList.sort();

    let selectedCategory = categories.get(selectedCategoryName);
    if (selectedCategory) {
        selectedCategory.attributes.sort((a, b) => {
            if (a.name < b.name) return -1;
            if (a.name > b.name) return 1;
            return 0;
        });
    }

    return <div>
        <button onClick={() => setShowCategories(!showCategories)}>{showCategories ? "-" : "+"}</button>categories
        {(showCategories) && (<div>
            select
            {categoriesList.map((k) => {
                return <button key={k} disabled={selectedCategoryName === k} onClick={() => { setSelectedCategory(k); console.log(`selecting ${k}`) }}>{k}</button>
            })}
        </div>)}
        <div><button onClick={() => setShowUpload(!showUpload)}>Upload {showUpload ? "-" : "+"} </button></div>
        {(showUpload) && <div>
            {selectedCategory == null ? "none category selected" : <div>
                <div>
                    fill upload form
                    <table ><tbody>
                        {selectedCategory.attributes.map((attr, index) => {
                            return <tr key={`${index}`}>
                                <td>{attr.name}</td>
                                <td><input onChange={(e) => {
                                    attributes.set(attr.name, e.target.value);
                                    console.log(attributes);
                                }} /></td>
                            </tr>;
                        })}
                    </tbody></table>
                </div>
                <div>
                    add file
                    <button onClick={() => setFiles([])}>Clear</button>
                    {`${files.length}`}
                    <input type="file" multiple onChange={(e) => { setFiles([...files, ...e.target.files]) }} />
                </div>
                <div>
                    upload
                    <button onClick={() => {
                        const formData = new FormData();
                        let uploadedAttributes = new Map();
                        uploadedAttributes.set("category", selectedCategoryName);
                        selectedCategory.attributes.forEach(({ type, name }) => {
                            if (attributes.has(name)) {
                                uploadedAttributes.set(name, attributes.get(name));
                            }
                        });
                        let requestText = JSON.stringify({
                            token: "eyJhbGciOiJIUzI1NiJ9.eyJsZXZlbCI6ImxvdyIsInVzZXJuYW1lIjoiYWxleCIsImV4cHIiOjE3MzExNzE4MDYwNzB9.cyot9TKDKMtHSgx7YuvnSDEqoaLrH-5jhQaGKtF3pT0",
                            merchandise: Object.fromEntries(uploadedAttributes)
                        });

                        formData.append("main", requestText);
                        files.forEach((f, i) => {
                            formData.append(`file${i}`, f);
                        });

                        fetch(`${Constants.BACKEND}/add_merchandise_2`, {
                            method: "POST",
                            body: formData,
                        }).then((res) => {
                            if (res.ok) {
                                console.log("success");
                                setMessage("upload success");
                                setSelectedCategory(null);
                                setAttributes(new Map());
                            }

                        }).catch(() => {
                            console.log("failed");
                        });


                    }}>upload</button>
                </div>
            </div>
            }
            <div>{message}</div>
        </div>}
        <div><button onClick={() => setShowSearch(!showSearch)}>Search {showSearch ? "-" : "+"}</button></div>
        {(showSearch) && <div>
            {selectedCategory == null ? "none category selected" : <div>
                <div>Fill search filters</div>
                <div><table ><tbody>
                    {selectedCategory.attributes.map((attr, index) => {
                        if (attr.type === "Float") {
                            return <tr key={`${index}`}>
                                <td>{attr.name}</td>
                                <td><input type="number" onChange={(e) => {
                                    if (!filters.has(attr.name)) {
                                        filters.set(attr.name, [null, null]);
                                    }
                                    let prev = filters.get(attr.name);
                                    prev[0] = parseFloat(e.target.value);
                                    filters.set(attr.name, prev);
                                }} /></td>
                                <td><input type="number" onChange={(e) => {
                                    if (!filters.has(attr.name)) {
                                        filters.set(attr.name, [null, null]);
                                    }
                                    let prev = filters.get(attr.name);
                                    prev[1] = parseFloat(e.target.value);
                                    filters.set(attr.name, prev);
                                }} /></td>
                            </tr>;
                        } else if (attr.type === "String") {
                            return <tr key={`${index}`}>
                                <td>{attr.name}</td>
                                <td colSpan="2" ><input style={{ width: "100%" }} onChange={(e) => {
                                    filters.set(attr.name, e.target.value);
                                }} /></td>

                            </tr>;
                        } else {
                            return null;
                        }
                    })}
                </tbody></table></div>
                <div><button onClick={() => {
                    let uploadedFilters = [];
                    selectedCategory.attributes.forEach(({ type, name }) => {
                        console.log(name);
                        if (filters.has(name)) {
                            uploadedFilters.push({ name: name, value: filters.get(name) });
                        }
                    });
                    console.log(uploadedFilters);
                    let body = JSON.stringify({
                        category: selectedCategoryName,
                        filters: uploadedFilters,
                    });
                    console.log(body);
                    fetch(`${Constants.BACKEND}/search_merchandise`, {
                        method: "POST",
                        body: body
                    }).then(response => {
                        response.text().then(text => {
                            setSearchResult(JSON.parse(text));
                        });
                    }).catch(() => {
                        console.log("failed");
                    });
                }}>search</button></div>
                <div>
                    Search results:
                    {searchResult.map(result => {
                        return <div><a
                            target="_blank" rel="noreferrer"
                            href={`/merchandise_detail?username=${result.username}&merchandise_id=${result.merchandise_id}`}
                        >{`${result.merchandise_id} by ${result.username}`}</a></div>;
                    })}

                </div>

            </div>}
        </div>

        }

    </div>;
}
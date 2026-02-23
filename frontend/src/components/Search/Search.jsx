import React from "react";
import "./Search.css";
import { assets } from "../../assets/assets";

const Search = () => {
  return (
    <div className="search_div">
      <h1>Претрага</h1>
      <div className="search_input">
        <input type="text" placeholder="Претрага" />
        <img src={assets.remove} alt="" />
      </div>
      <div className="search_profiles"></div>
    </div>
  );
};

export default Search;

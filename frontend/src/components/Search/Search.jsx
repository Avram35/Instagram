import React, { useEffect, useState } from "react";
import "./Search.css";
import { assets } from "../../assets/assets";
import { useNavigate } from "react-router-dom";
import { searchUsers, getUserAvatarUrl } from "../../api/userApi";

const Search = ({ setSearchNotification }) => {
  const [search, setSearch] = useState("");
  const [searchProfiles, setSearchProfiles] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    if (search.trim() === "") {
      setSearchProfiles([]);
      return;
    }

    const fetchUsers = async () => {
      try {
        const data = await searchUsers(search);
        setSearchProfiles(data);
      } catch (error) {
        console.error("Error searching users:", error);
      }
    };

    const timeout = setTimeout(fetchUsers, 400);
    return () => clearTimeout(timeout);
  }, [search]);

  return (
    <div className="search_div">
      <h1>Претрага</h1>
      <div className="search_input">
        <input
          type="text"
          placeholder="Претрага"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <img src={assets.remove} alt="" onClick={() => setSearch("")} />
      </div>
      <div className="search_profiles">
        {searchProfiles.map((prof, index) => (
          <div
            className="search_profile"
            key={index}
            onClick={() => {
              navigate(`/profile/${prof.username}`);
              setSearchNotification(false);
            }}
          >
            <img
              src={getUserAvatarUrl(
                prof.profilePictureUrl,
                assets.noProfilePic,
              )}
              alt=""
            />
            <div className="search_profile_info">
              <span className="search_username">{prof.username}</span>
              <span className="search_name">
                {prof.fname} {prof.lname}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Search;

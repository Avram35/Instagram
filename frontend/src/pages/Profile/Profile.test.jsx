import { render, screen, cleanup, act } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Profile from "./Profile";
import { AppContext } from "../../context/AppContext";

const mockProfileInfo = {
  id: 1,
  username: "mihajlotim",
  fname: "Mihajlo",
  lname: "Timotijevic",
  bio: "Ja sam Mihajlo i vozim biciklu.",
  profilePictureUrl: null,
  privateProfile: false,
};

const mockFollowCount = {
  followersCount: 10,
  followingCount: 5,
};

const mockUser = {
  username: "mihajlotim",
};

beforeEach(() => {
  global.fetch = vi.fn((url) => {
    if (url.includes("/api/v1/user")) {
      return Promise.resolve({
        json: () => Promise.resolve(mockProfileInfo),
      });
    }
    if (url.includes("/api/v1/follow")) {
      return Promise.resolve({
        json: () => Promise.resolve(mockFollowCount),
      });
    }
  });

  localStorage.setItem("token", "test-token");
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
  localStorage.clear();
});

const renderProfile = async (
  username = "mihajlotim",
  contextUser = mockUser,
) => {
  await act(async () => {
    render(
      <MemoryRouter initialEntries={[`/profile/${username}`]}>
        <AppContext.Provider value={{ user: contextUser }}>
          <Routes>
            <Route path="/profile/:username" element={<Profile />} />
          </Routes>
        </AppContext.Provider>
      </MemoryRouter>,
    );
  });
};

describe("Profile", () => {
  it("prikazuje loading dok se ucitava profil", () => {
    global.fetch = vi.fn(() => new Promise(() => {}));
    render(
      <MemoryRouter initialEntries={["/profile/mihajlotim"]}>
        <AppContext.Provider value={{ user: mockUser }}>
          <Routes>
            <Route path="/profile/:username" element={<Profile />} />
          </Routes>
        </AppContext.Provider>
      </MemoryRouter>,
    );
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("prikazuje informacije o profilu", async () => {
    await renderProfile();
    expect(screen.getByText("mihajlotim")).toBeInTheDocument();
    expect(screen.getByText("Mihajlo Timotijevic")).toBeInTheDocument();
    expect(
      screen.getByText("Ja sam Mihajlo i vozim biciklu."),
    ).toBeInTheDocument();
  });

  it("prikazuje broj pratilaca i pracenja", async () => {
    await renderProfile();
    expect(screen.getByText("10")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
  });

  it("prikazuje dugme Izmeni profil za sopstveni profil", async () => {
    await renderProfile();
    expect(screen.getByText("Измените профил")).toBeInTheDocument();
  });

  it("prikazuje dugme Prati za tudji profil", async () => {
    await renderProfile("mihajlotim", { username: "otheruser" });
    expect(screen.getByText("Прати")).toBeInTheDocument();
  });

  it("prikazuje da je nalog javan", async () => {
    await renderProfile();
    expect(screen.getByText("Налог је јаван")).toBeInTheDocument();
  });

  it("prikazuje da je nalog privatan", async () => {
    global.fetch = vi.fn((url) => {
      if (url.includes("/api/v1/user")) {
        return Promise.resolve({
          json: () =>
            Promise.resolve({ ...mockProfileInfo, privateProfile: true }),
        });
      }
      return Promise.resolve({
        json: () => Promise.resolve(mockFollowCount),
      });
    });

    await renderProfile();
    expect(screen.getByText("Налог је приватан")).toBeInTheDocument();
  });
});

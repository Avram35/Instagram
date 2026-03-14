import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import Navbar from "./Navbar";
import { AppContext } from "../../context/AppContext";

vi.mock("../../api/followApi", () => ({
  fetchNotifications: vi.fn(() => Promise.resolve([])),
  fetchPendingRequests: vi.fn(() => Promise.resolve([])),
  markNotificationsAsRead: vi.fn(() => Promise.resolve()),
}));

vi.mock("../../api/userApi", () => ({
  getUserAvatarUrl: vi.fn(() => "mocked-avatar-url"),
}));

vi.mock("../../assets/assets", () => ({
  assets: {
    logo: "mocked-logo",
    home: "mocked-home",
    home1: "mocked-home1",
    search: "mocked-search",
    search1: "mocked-search1",
    heart: "mocked-heart",
    heart1: "mocked-heart1",
    plus: "mocked-plus",
    plus1: "mocked-plus1",
    menu: "mocked-menu",
    menu1: "mocked-menu1",
    noProfilePic: "mocked-no-profile-pic",
  },
}));

const mockUser = {
  username: "mihajlotim",
  profilePictureUrl: "",
};

const mockProps = {
  setSearchNotification: vi.fn(),
  searchNotification: null,
  morePanel: false,
  setMorePanel: vi.fn(),
  panRef: { current: document.createElement("div") },
  morePanRef: { current: document.createElement("div") },
  setCreatePost: vi.fn(),
  createPost: false,
};

const renderNavbar = async (props = {}) => {
  await act(async () => {
    render(
      <MemoryRouter initialEntries={["/"]}>
        <AppContext.Provider value={{ user: mockUser }}>
          <Navbar {...mockProps} {...props} />
        </AppContext.Provider>
      </MemoryRouter>,
    );
  });
};

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});

describe("Navbar", () => {
  it("prikazuje navbar elemente", async () => {
    await renderNavbar();
    expect(screen.getByText("Почетак")).toBeInTheDocument();
    expect(screen.getByText("Претрага")).toBeInTheDocument();
    expect(screen.getByText("Обавештења")).toBeInTheDocument();
    expect(screen.getByText("Објави")).toBeInTheDocument();
    expect(screen.getByText("Профил")).toBeInTheDocument();
    expect(screen.getByText("Још")).toBeInTheDocument();
  });

  it("klik na Претрага poziva setSearchNotification", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Претрага"));
    expect(mockProps.setSearchNotification).toHaveBeenCalledWith("pretraga");
  });

  it("klik na Обавештења poziva setSearchNotification", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Обавештења"));
    expect(mockProps.setSearchNotification).toHaveBeenCalledWith("obavestenja");
  });

  it("klik na Још poziva setMorePanel", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Још"));
    expect(mockProps.setMorePanel).toHaveBeenCalled();
  });

  it("klik na Објави poziva setCreatePost", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Објави"));
    expect(mockProps.setCreatePost).toHaveBeenCalledWith(true);
    expect(mockProps.setSearchNotification).toHaveBeenCalledWith(null);
    expect(mockProps.setMorePanel).toHaveBeenCalledWith(false);
  });

  it("klik na Почетак poziva setSearchNotification sa null", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Почетак"));
    expect(mockProps.setSearchNotification).toHaveBeenCalledWith(null);
  });

  it("prikazuje crveni indikator kad ima neprocitanih obavestenja", async () => {
    const { fetchNotifications } = await import("../../api/followApi");
    vi.mocked(fetchNotifications).mockResolvedValueOnce([
      { id: 1, read: false },
    ]);

    await renderNavbar();
    expect(document.querySelector(".unread_notifications")).toBeInTheDocument();
  });

  it("ne prikazuje crveni indikator kad su sva obavenstenja procitana", async () => {
    const { fetchNotifications } = await import("../../api/followApi");
    vi.mocked(fetchNotifications).mockResolvedValueOnce([
      { id: 1, read: true },
    ]);

    await renderNavbar();
    expect(
      document.querySelector(".unread_notifications"),
    ).not.toBeInTheDocument();
  });
});

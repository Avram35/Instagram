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

const mockUser = {
  username: "testuser",
  profilePictureUrl: "",
};

const mockProps = {
  setSearchNotification: vi.fn(),
  searchNotification: null,
  morePanel: false,
  setMorePanel: vi.fn(),
  panRef: { current: document.createElement("div") },
  morePanRef: { current: document.createElement("div") },
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

  it("klik na Објави postavlja active na objavi", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Објави"));
    expect(mockProps.setSearchNotification).toHaveBeenCalledWith(null);
    expect(mockProps.setMorePanel).toHaveBeenCalledWith(false);
  });

  it("klik na Почетак poziva setSearchNotification sa null", async () => {
    await renderNavbar();
    fireEvent.click(screen.getByText("Почетак"));
    expect(mockProps.setSearchNotification).toHaveBeenCalledWith(null);
  });
});

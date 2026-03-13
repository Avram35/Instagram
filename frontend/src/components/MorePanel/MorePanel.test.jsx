import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import MorePanel from "./MorePanel";
import { AppContext } from "../../context/AppContext";

const mockLogout = vi.fn();
const mockSetMorePanel = vi.fn();
const morePanRef = { current: document.createElement("div") };

const renderMorePanel = async (morePanel = true) => {
  await act(async () => {
    render(
      <AppContext.Provider value={{ logout: mockLogout }}>
        <MorePanel
          morePanel={morePanel}
          morePanRef={morePanRef}
          setMorePanel={mockSetMorePanel}
        />
      </AppContext.Provider>,
    );
  });
};

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
});

describe("MorePanel", () => {
  it("prikazuje dugme za odjavu kad je panel otvoren", async () => {
    await renderMorePanel(true);
    expect(screen.getByText("Одјавите се")).toBeInTheDocument();
  });

  it("ne prikazuje dugme za odjavu kad je panel zatvoren", async () => {
    await renderMorePanel(false);
    expect(screen.queryByText("Одјавите се")).not.toBeInTheDocument();
  });

  it("poziva logout i setMorePanel klikom na Одјавите се", async () => {
    await renderMorePanel(true);
    fireEvent.click(screen.getByText("Одјавите се"));
    expect(mockLogout).toHaveBeenCalled();
    expect(mockSetMorePanel).toHaveBeenCalledWith(false);
  });
});

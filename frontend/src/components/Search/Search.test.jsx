import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
  waitFor,
} from "@testing-library/react";
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import Search from "./Search";

const mockSetSearchNotification = vi.fn();

const mockProfiles = [
  {
    username: "mihajlotim",
    fname: "Mihajlo",
    lname: "Timotijevic",
    profilePictureUrl: null,
  },
  {
    username: "milanavram",
    fname: "Milan",
    lname: "Avramovic",
    profilePictureUrl: null,
  },
];

const renderSearch = async () => {
  await act(async () => {
    render(
      <MemoryRouter>
        <Search setSearchNotification={mockSetSearchNotification} />
      </MemoryRouter>,
    );
  });
};

beforeEach(() => {
  global.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve(mockProfiles),
    }),
  );
  localStorage.setItem("token", "test-token");
  vi.clearAllMocks();
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe("Search", () => {
  it("prikazuje naslov Претрага", async () => {
    await renderSearch();
    expect(screen.getByText("Претрага")).toBeInTheDocument();
  });

  it("prikazuje input za pretragu", async () => {
    await renderSearch();
    expect(screen.getByPlaceholderText("Претрага")).toBeInTheDocument();
  });

  it("ne prikazuje profile kad je input prazan", async () => {
    await renderSearch();
    expect(screen.queryByText("mihajlotim")).not.toBeInTheDocument();
  });

  it("prikazuje profile nakon pretrage", async () => {
    await renderSearch();
    await act(async () => {
      fireEvent.change(screen.getByPlaceholderText("Претрага"), {
        target: { value: "mihajlo" },
      });
    });
    await waitFor(
      () => {
        expect(screen.getByText("mihajlotim")).toBeInTheDocument();
      },
      { timeout: 1000 },
    );
  });

  it("brise pretragu klikom na x", async () => {
    await renderSearch();
    const input = screen.getByPlaceholderText("Претрага");
    fireEvent.change(input, { target: { value: "mihajlo" } });
    fireEvent.click(screen.getByAltText(""));
    expect(input).toHaveValue("");
  });

  it("poziva fetch sa ispravnim query parametrom", async () => {
    await renderSearch();
    await act(async () => {
      fireEvent.change(screen.getByPlaceholderText("Претрага"), {
        target: { value: "mihajlo" },
      });
    });
    await waitFor(
      () => {
        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining("query=mihajlo"),
          expect.any(Object),
        );
      },
      { timeout: 1000 },
    );
  });
});

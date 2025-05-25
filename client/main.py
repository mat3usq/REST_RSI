import tkinter as tk
from tkinter import ttk
import requests
from datetime import datetime
from requests.auth import HTTPBasicAuth

requests.packages.urllib3.disable_warnings()


class EventsClient:
    def __init__(self, root):
        self.root = root
        self.root.title("Białystok Events Client")
        self.root.geometry("910x400")
        self.root.minsize(910, 400)
        self.center_window(self.root, 910, 400)
        self.style = ttk.Style()
        self.style.configure("Treeview.Heading", font=('Helvetica', 10, 'bold'))
        self.style.configure("Treeview", rowheight=25)

        self.base_url = "https://localhost:8181/bialystok-info/api/events"
        self.auth = HTTPBasicAuth('admin', 'admin')

        self.name_var = tk.StringVar()
        self.type_var = tk.StringVar()
        self.date_var = tk.StringVar()
        self.time_var = tk.StringVar()
        self.search_by_week_var = tk.BooleanVar()

        self.create_widgets()
        self.load_all_events()

    def create_widgets(self):
        main_frame = ttk.Frame(self.root, padding=10)
        main_frame.pack(fill=tk.BOTH, expand=True)

        search_frame = ttk.Frame(main_frame)
        search_frame.pack(fill=tk.X, pady=5)

        ttk.Label(search_frame, text="Date (YYYY-MM-DD):").pack(side=tk.LEFT, padx=5)
        self.date_entry = ttk.Entry(search_frame, width=15, textvariable=self.date_var)
        self.date_entry.pack(side=tk.LEFT, padx=5)

        self.search_by_week_checkbox = ttk.Checkbutton(search_frame, text="Search by Week",
                                                       variable=self.search_by_week_var)
        self.search_by_week_checkbox.pack(side=tk.LEFT, padx=5)

        ttk.Button(search_frame, text="Search", command=self.search_events, width=10).pack(side=tk.LEFT, padx=5)
        ttk.Button(search_frame, text="Generate PDF", command=self.generate_pdf, width=12).pack(side=tk.RIGHT, padx=5)
        ttk.Button(search_frame, text="Add Event", command=self.open_add_event_form, width=12).pack(side=tk.RIGHT,
                                                                                                    padx=5)

        self.results_tree = ttk.Treeview(main_frame, columns=("ID", "Name", "Type", "Date"), show="headings", height=15)
        columns_config = {
            "ID": {"width": 40, "anchor": tk.CENTER, "stretch": False},
            "Name": {"width": 180, "anchor": tk.CENTER},
            "Type": {"width": 100, "anchor": tk.CENTER},
            "Date": {"width": 120, "anchor": tk.CENTER}
        }

        for col, config in columns_config.items():
            self.results_tree.column(col, **config)
            self.results_tree.heading(col, text=col, anchor=tk.CENTER)

        scroll = ttk.Scrollbar(main_frame, orient="vertical", command=self.results_tree.yview)
        self.results_tree.configure(yscrollcommand=scroll.set)
        self.results_tree.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scroll.pack(side=tk.RIGHT, fill=tk.Y)
        self.results_tree.bind("<Double-1>", self.show_event_details)

    def load_all_events(self):
        try:
            response = requests.get(self.base_url, auth=self.auth, verify=False)
            response.raise_for_status()
            self.display_events(response.json())
        except requests.exceptions.ConnectionError:
            self.show_error("Cannot connect to server. Check if server is running.")
        except Exception as e:
            self.show_error(f"Error loading events: {str(e)}")

    def search_events(self):
        date = self.date_var.get()
        try:
            if self.search_by_week_var.get():
                date_obj = datetime.strptime(date, "%Y-%m-%d")
                week_number = date_obj.isocalendar()[1]
                year = date_obj.year
                params = {'week': week_number, 'year': year}
                response = requests.get(f"{self.base_url}/week", params=params, auth=self.auth, verify=False)
            else:
                if not date.strip():
                    response = requests.get(self.base_url, auth=self.auth, verify=False)
                else:
                    response = requests.get(f"{self.base_url}/date/{date}", auth=self.auth, verify=False)

            response.raise_for_status()
            self.display_events(response.json())
        except Exception as e:
            self.show_error(f"Search error: {str(e)}")

    def display_events(self, events):
        self.results_tree.delete(*self.results_tree.get_children())
        for event in events:
            date_time = datetime.fromisoformat(event['dateTime']).strftime("%Y-%m-%d %H:%M")
            self.results_tree.insert("", "end", values=(
                event['id'],
                event['name'],
                event['type'],
                date_time
            ))

    def show_event_details(self, event):
        try:
            item = self.results_tree.focus()
            event_id = self.results_tree.item(item)['values'][0]
            response = requests.get(f"{self.base_url}/{event_id}", auth=self.auth, verify=False)
            response.raise_for_status()
            event = response.json()

            details_win = tk.Toplevel(self.root)
            details_win.title(f"Event Details - ID: {event['id']}")
            details_win.geometry("500x500")
            self.center_window(details_win, 500, 500)
            details_win.wait_visibility()
            details_win.grab_set()

            main_frame = ttk.Frame(details_win, padding=15)
            main_frame.pack(fill=tk.BOTH, expand=True)

            info_frame = ttk.Frame(main_frame)
            info_frame.pack(fill=tk.X, pady=5)

            label_font = 'Arial 12 bold'
            value_font = 'Arial 14'

            labels = [
                ("ID:", event['id']),
                ("Name:", event['name']),
                ("Type:", event['type']),
                ("Date:", datetime.fromisoformat(event['dateTime']).strftime("%Y-%m-%d %H:%M"))
            ]

            for row, (label, value) in enumerate(labels):
                ttk.Label(info_frame, text=label, font=label_font).grid(row=row, column=0, sticky='e', padx=5, pady=2)
                ttk.Label(info_frame, text=value, font=value_font).grid(row=row, column=1, sticky='w', padx=5, pady=2)

            desc_frame = ttk.LabelFrame(main_frame, text="Description", padding=10)
            desc_frame.pack(fill=tk.BOTH, expand=True, pady=10)

            text_scroll = ttk.Scrollbar(desc_frame)
            text_scroll.pack(side=tk.RIGHT, fill=tk.Y)

            desc_text = tk.Text(desc_frame, wrap=tk.WORD, height=8, yscrollcommand=text_scroll.set,
                                padx=5, pady=5, font='Arial 13')
            desc_text.insert(tk.END, event['description'])
            desc_text.config(state=tk.DISABLED)
            desc_text.pack(fill=tk.BOTH, expand=True)
            text_scroll.config(command=desc_text.yview)

            button_frame = ttk.Frame(main_frame)
            button_frame.pack(pady=10)
            ttk.Button(button_frame, text="Modify", command=lambda: self.open_edit_event_form(event, details_win)).pack(
                side=tk.LEFT, padx=5)
            ttk.Button(button_frame, text="Delete", command=lambda: self.delete_event(event['id'], details_win)).pack(
                side=tk.LEFT, padx=5)
            ttk.Button(button_frame, text="Close", command=details_win.destroy).pack(side=tk.RIGHT, padx=5)

        except Exception as e:
            self.show_error(f"Error loading details: {str(e)}")

    def open_add_event_form(self):
        add_win = tk.Toplevel(self.root)
        add_win.title("Add Event")
        add_win.geometry("600x260")
        add_win.resizable(False, False)
        self.center_window(add_win, 600, 260)
        add_win.grab_set()

        main_frame = ttk.Frame(add_win, padding=15)
        main_frame.pack(fill=tk.BOTH, expand=True)

        self.name_var.set("")
        self.type_var.set("")
        self.date_var.set("")
        self.time_var.set("")

        ttk.Label(main_frame, text="Name:").grid(row=0, column=0, sticky='e', padx=5, pady=2)
        ttk.Entry(main_frame, textvariable=self.name_var, width=40).grid(row=0, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Type:").grid(row=1, column=0, sticky='e', padx=5, pady=2)
        event_types = self.fetch_event_types()
        type_combobox = ttk.Combobox(main_frame, textvariable=self.type_var, values=event_types, state='readonly')
        type_combobox.grid(row=1, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Date (YYYY-MM-DD):").grid(row=2, column=0, sticky='e', padx=5, pady=2)
        ttk.Entry(main_frame, textvariable=self.date_var, width=40).grid(row=2, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Time (HH:MM):").grid(row=3, column=0, sticky='e', padx=5, pady=2)
        ttk.Entry(main_frame, textvariable=self.time_var, width=40).grid(row=3, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Description:").grid(row=4, column=0, sticky='ne', padx=5, pady=2)
        add_desc_text = tk.Text(main_frame, wrap=tk.WORD, height=4, width=40)
        add_desc_text.grid(row=4, column=1, padx=5, pady=2)

        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=5, column=0, columnspan=2, pady=10)
        ttk.Button(button_frame, text="Add Event", command=lambda: self.add_event(add_win, add_desc_text)).pack(
            side=tk.LEFT, padx=10)
        ttk.Button(button_frame, text="Cancel", command=add_win.destroy).pack(side=tk.RIGHT, padx=10)

    def fetch_event_types(self):
        try:
            response = requests.get(f"{self.base_url}/types", auth=self.auth, verify=False)
            response.raise_for_status()
            return response.json()
        except Exception as e:
            self.show_error(f"Error fetching types: {str(e)}. Using defaults.")
            return ['CONCERT', 'FESTIVAL', 'FAIR', 'GASTRONOMY', 'HISTORY', 'LITERARY', 'FOR_CHILDREN']

    def add_event(self, add_win, desc_text_widget):
        try:
            datetime.strptime(self.date_var.get(), "%Y-%m-%d")
            datetime.strptime(self.time_var.get(), "%H:%M")

            event_data = {
                "name": self.name_var.get(),
                "type": self.type_var.get(),
                "dateTime": f"{self.date_var.get()}T{self.time_var.get()}:00",
                "description": desc_text_widget.get("1.0", tk.END).strip()
            }

            response = requests.post(
                self.base_url,
                json=event_data,
                auth=self.auth,
                verify=False,
                headers={'Content-Type': 'application/json'}
            )

            response.raise_for_status()
            self.show_info("Event added successfully!")
            self.load_all_events()
            add_win.destroy()

        except ValueError as e:
            self.show_error(f"Invalid date/time format: {str(e)}")
        except Exception as e:
            self.show_error(f"Add error: {str(e)}")

    def generate_pdf(self):
        try:
            params = {'byWeek': self.search_by_week_var.get()}
            date_value = self.date_var.get()

            if date_value.strip():
                params['date'] = date_value

            headers = {'Accept': 'application/pdf'}
            response = requests.get(
                f"{self.base_url}/report",
                params=params if date_value.strip() else {'byWeek': False},
                auth=self.auth,
                verify=False,
                headers=headers
            )

            response.raise_for_status()

            with open("events_report.pdf", "wb") as f:
                f.write(response.content)

            self.show_info("PDF saved as 'events_report.pdf'")

        except Exception as e:
            self.show_error(f"PDF error: {str(e)}")

    def show_info(self, msg):
        info_win = tk.Toplevel(self.root)
        info_win.title("Info")
        info_win.resizable(False, False)
        self.center_window(info_win, 360, 110)
        ttk.Label(info_win, text=msg, padding=10).pack()
        ttk.Button(info_win, text="OK", command=info_win.destroy).pack(pady=10)
        info_win.grab_set()

    def show_error(self, msg):
        error_win = tk.Toplevel(self.root)
        error_win.title("Error")
        error_win.resizable(False, False)
        self.center_window(error_win, 360, 110)
        ttk.Label(error_win, text=msg, padding=10).pack()
        ttk.Button(error_win, text="OK", command=error_win.destroy).pack(pady=10)
        error_win.wait_visibility()
        error_win.grab_set()

    def center_window(self, window, width=300, height=200):
        screen_width = window.winfo_screenwidth()
        screen_height = window.winfo_screenheight()
        x = (screen_width / 2) - (width / 2)
        y = (screen_height / 2) - (height / 2)
        window.geometry('%dx%d+%d+%d' % (width, height, x, y))

    def delete_event(self, event_id, details_win):
        try:
            response = requests.delete(f"{self.base_url}/{event_id}", auth=self.auth, verify=False)
            response.raise_for_status()
            self.show_info("Event deleted successfully!")
            self.load_all_events()
            details_win.destroy()
        except Exception as e:
            self.show_error(f"Error deleting event: {str(e)}")

    def open_edit_event_form(self, event, details_win):
        edit_win = tk.Toplevel(self.root)
        edit_win.title("Edit Event")
        edit_win.geometry("600x260")
        edit_win.resizable(False, False)
        self.center_window(edit_win, 600, 260)
        edit_win.grab_set()

        main_frame = ttk.Frame(edit_win, padding=15)
        main_frame.pack(fill=tk.BOTH, expand=True)

        self.name_var.set(event['name'])
        self.type_var.set(event['type'])
        self.date_var.set(event['dateTime'].split("T")[0])
        self.time_var.set(event['dateTime'].split("T")[1][:5])

        ttk.Label(main_frame, text="Name:").grid(row=0, column=0, sticky='e', padx=5, pady=2)
        ttk.Entry(main_frame, textvariable=self.name_var, width=40).grid(row=0, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Type:").grid(row=1, column=0, sticky='e', padx=5, pady=2)
        event_types = self.fetch_event_types()
        type_combobox = ttk.Combobox(main_frame, textvariable=self.type_var, values=event_types, state='readonly')
        type_combobox.grid(row=1, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Date (YYYY-MM-DD):").grid(row=2, column=0, sticky='e', padx=5, pady=2)
        ttk.Entry(main_frame, textvariable=self.date_var, width=40).grid(row=2, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Time (HH:MM):").grid(row=3, column=0, sticky='e', padx=5, pady=2)
        ttk.Entry(main_frame, textvariable=self.time_var, width=40).grid(row=3, column=1, padx=5, pady=2)

        ttk.Label(main_frame, text="Description:").grid(row=4, column=0, sticky='ne', padx=5, pady=2)
        edit_desc_text = tk.Text(main_frame, wrap=tk.WORD, height=4, width=40)
        edit_desc_text.insert(tk.END, event['description'])
        edit_desc_text.grid(row=4, column=1, padx=5, pady=2)

        button_frame = ttk.Frame(main_frame)
        button_frame.grid(row=5, column=0, columnspan=2, pady=10)
        ttk.Button(button_frame, text="Save Changes",
                   command=lambda: self.update_event(event['id'], edit_desc_text, edit_win)).pack(side=tk.LEFT, padx=10)
        ttk.Button(button_frame, text="Cancel", command=edit_win.destroy).pack(side=tk.RIGHT, padx=10)

        details_win.destroy()

    def update_event(self, event_id, desc_text_widget, edit_win):
        try:
            event_data = {
                "name": self.name_var.get(),
                "type": self.type_var.get(),
                "dateTime": f"{self.date_var.get()}T{self.time_var.get()}:00",
                "description": desc_text_widget.get("1.0", tk.END).strip()
            }

            if not all([event_data['name'], event_data['type'], event_data['dateTime'], event_data['description']]):
                self.show_error("All fields are required!")
                return

            response = requests.put(
                f"{self.base_url}/{event_id}",
                json=event_data,
                auth=self.auth,
                verify=False,
                headers={'Content-Type': 'application/json'}
            )

            response.raise_for_status()

            self.show_info("Event updated successfully!")
            self.load_all_events()
            edit_win.destroy()

        except requests.exceptions.RequestException as e:
            self.show_error(f"Update failed: {str(e)}")
        except Exception as e:
            self.show_error(f"Error: {str(e)}")


if __name__ == "__main__":
    root = tk.Tk()
    app = EventsClient(root)
    root.mainloop()
